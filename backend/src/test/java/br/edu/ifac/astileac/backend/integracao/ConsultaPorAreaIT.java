package br.edu.ifac.astileac.backend.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * A consulta do RF08 contra o banco de verdade.
 *
 * E JPQL escrito a mao, com duas decisoes que so o banco confirma: o
 * EXISTS no lugar do JOIN, para quem atua em duas areas nao aparecer
 * duplicado, e o CAST nos parametros, sem o qual o filtro nulo nem
 * chega a executar. Teste de unidade com repositorio simulado nao roda
 * consulta nenhuma.
 */
class ConsultaPorAreaIT extends IntegracaoBase {

	@Autowired
	private ProfissionalRepository profissionalRepository;

	@Autowired
	private Associados associados;

	@Test
	void quemAtuaEmDuasAreasApareceUmaVezSoEAContagemBate() {
		Long maria = associados.porConvite("Maria Silva", "11144477735", "maria.silva");
		associados.atuaEm(maria, "juridica", "saude");
		Long joao = associados.porConvite("Joao Pereira", "52998224725", "joao.pereira");
		associados.atuaEm(joao, "educacional");

		// Com JOIN, a listagem sem filtro de area traria a Maria uma vez
		// por area -- duas linhas -- e a contagem da paginacao diria 3.
		Page<Profissional> todos = consultar(null, null, null);
		assertEquals(2, todos.getTotalElements());
		assertEquals(2, todos.getContent().size());

		Page<Profissional> juridica = consultar(null, null, "juridica");
		assertEquals(1, juridica.getTotalElements());
		assertEquals(List.of("Maria Silva"), nomes(juridica));
	}

	@Test
	void filtrosSeCombinamENomeIgnoraMaiusculas() {
		Long maria = associados.porConvite("Maria Silva", "11144477735", "maria.silva");
		associados.atuaEm(maria, "juridica");
		Long marina = associados.porConvite("Marina Souza", "52998224725", "marina.souza");
		associados.atuaEm(marina, "juridica");
		jdbc.update("UPDATE profissional SET ativo = false WHERE id = ?", marina);
		Long joao = associados.porConvite("Joao Pereira", "12345678909", "joao.pereira");
		associados.atuaEm(joao, "educacional");

		assertEquals(List.of("Maria Silva", "Marina Souza"), nomes(consultar("MAR", null, null)));
		assertEquals(List.of("Maria Silva"), nomes(consultar("mar", true, "juridica")));
		assertEquals(List.of("Marina Souza"), nomes(consultar(null, false, null)));
		assertEquals(List.of(), nomes(consultar("joao", null, "juridica")));
	}

	/**
	 * Bloqueio da LGPD (Art. 18, IV): quem pediu exclusao some da
	 * consulta mesmo atendendo a todos os filtros -- e volta se desistir.
	 */
	@Test
	void quemPediuExclusaoNaoAparece() {
		Long maria = associados.porConvite("Maria Silva", "11144477735", "maria.silva");
		associados.atuaEm(maria, "juridica");

		jdbc.update("UPDATE profissional SET data_exclusao_solicitada = now() WHERE id = ?", maria);
		assertEquals(0, consultar("maria", null, "juridica").getTotalElements());

		jdbc.update("UPDATE profissional SET data_exclusao_solicitada = NULL WHERE id = ?", maria);
		assertEquals(1, consultar("maria", null, "juridica").getTotalElements());
	}

	private Page<Profissional> consultar(String nome, Boolean ativo, String area) {
		return profissionalRepository.consultar(nome, ativo, area, PageRequest.of(0, 20));
	}

	private List<String> nomes(Page<Profissional> pagina) {
		return pagina.getContent().stream().map(Profissional::getNome).sorted().toList();
	}

}
