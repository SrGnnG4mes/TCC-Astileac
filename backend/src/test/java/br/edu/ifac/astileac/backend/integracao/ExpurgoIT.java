package br.edu.ifac.astileac.backend.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifac.astileac.backend.service.DadosPessoaisService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Exclusao definitiva depois da janela de arrependimento (RF10, Art. 18).
 *
 * O expurgo apaga em varias tabelas, numa ordem imposta pelas chaves
 * estrangeiras, e roda as 3h da manha sem ninguem olhando. Teste de
 * unidade confere que os deletes foram chamados; so o banco diz se a
 * ordem e a lista estao certas -- foi assim que a falta de USUARIO e
 * CONVITE passou despercebida, ate ser achada lendo o codigo.
 */
class ExpurgoIT extends IntegracaoBase {

	/** Toda tabela que aponta para profissional. */
	private static final List<String> TABELAS_DO_PROFISSIONAL = List.of(
			"endereco", "contato", "profissional_area", "formacao", "certificado",
			"vinculo_empregaticio", "usuario", "convite");

	@Autowired
	private DadosPessoaisService dadosPessoaisService;

	@Autowired
	private Associados associados;

	@Value("${app.uploads.dir}")
	private String pastaDeUploads;

	@Test
	void pedidoVencidoApagaTudoDaPessoaInclusiveContaConviteEDocumento() {
		Long id = associados.completo("Maria Silva", "11144477735", "maria.silva");
		Path documento = documentoDe(id);
		assertTrue(Files.exists(documento));

		pedirExclusaoHa(id, 31);
		assertEquals(1, dadosPessoaisService.expurgarPrazoVencido());

		for (String tabela : TABELAS_DO_PROFISSIONAL) {
			assertEquals(0, contar("SELECT count(*) FROM " + tabela + " WHERE profissional_id = ?", id),
					"sobrou linha em " + tabela);
		}
		assertEquals(0, contar("SELECT count(*) FROM profissional WHERE id = ?", id));

		// O PDF e dado do titular tanto quanto as linhas do banco.
		assertFalse(Files.exists(documento), "o documento do certificado sobreviveu a exclusao definitiva");

		// Fica o registro de que a eliminacao aconteceu, sem dado pessoal.
		assertEquals(1, contar(
				"SELECT count(*) FROM log_acesso WHERE acao = 'EXCLUSAO' AND registro_id = ?", id));
	}

	/**
	 * Os 30 dias sao garantia de MINIMO: apagar depois e aceitavel,
	 * apagar antes nao.
	 */
	@Test
	void soQuemPassouDoPrazoEApagado() {
		Long vencido = associados.completo("Maria Silva", "11144477735", "maria.silva");
		Long noPrazo = associados.completo("Joao Pereira", "52998224725", "joao.pereira");
		Path documentoNoPrazo = documentoDe(noPrazo);

		pedirExclusaoHa(vencido, 31);
		pedirExclusaoHa(noPrazo, 29);

		assertEquals(1, dadosPessoaisService.expurgarPrazoVencido());

		assertEquals(0, contar("SELECT count(*) FROM profissional WHERE id = ?", vencido));
		assertEquals(1, contar("SELECT count(*) FROM profissional WHERE id = ?", noPrazo));
		for (String tabela : TABELAS_DO_PROFISSIONAL) {
			assertEquals(1, contar("SELECT count(*) FROM " + tabela + " WHERE profissional_id = ?", noPrazo),
					"quem ainda esta no prazo perdeu dado em " + tabela);
		}
		assertTrue(Files.exists(documentoNoPrazo));
	}

	private void pedirExclusaoHa(Long id, int dias) {
		jdbc.update("""
				UPDATE profissional
				SET data_exclusao_solicitada = now() - make_interval(days => ?), ativo = false
				WHERE id = ?
				""", dias, id);
	}

	private Path documentoDe(Long id) {
		String caminho = jdbc.queryForObject(
				"SELECT arquivo_path FROM certificado WHERE profissional_id = ?", String.class, id);
		return Path.of(pastaDeUploads).toAbsolutePath().resolve(caminho).normalize();
	}

}
