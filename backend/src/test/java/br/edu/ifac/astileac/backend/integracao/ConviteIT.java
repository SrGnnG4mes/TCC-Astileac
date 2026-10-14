package br.edu.ifac.astileac.backend.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifac.astileac.backend.dto.CadastroPorConviteDTO;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.CpfJaCadastradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import br.edu.ifac.astileac.backend.service.ConviteService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Convite de uso unico (RF12) contra o banco de verdade.
 *
 * Os tres casos sao propriedades que so existem com persistencia real:
 * o que fica gravado depois que a transacao confirma, o que e desfeito
 * quando ela falha, e quem ganha quando duas transacoes disputam a
 * mesma linha.
 */
class ConviteIT extends IntegracaoBase {

	@Autowired
	private ConviteService conviteService;

	@Autowired
	private ProfissionalRepository profissionalRepository;

	/**
	 * O defeito encontrado rodando: o UPDATE em massa marcava o consumo no
	 * banco, e o save() do fim do metodo gravava por cima a entidade
	 * obsoleta, com o consumo nulo. O convite continuava valido.
	 *
	 * Este teste falha com aquele codigo -- conferido removendo a
	 * correcao -- e e o teste que faltava quando so havia os de unidade.
	 */
	@Test
	void conviteUsadoNaoServeUmaSegundaVez() {
		String token = conviteService.emitir("maria@exemplo.com", "diretora").token();

		conviteService.cadastrar(cadastro(token, "11144477735", "maria.silva"));

		// O que o defeito apagava: o consumo gravado no banco.
		assertNotNull(jdbc.queryForObject("SELECT data_consumo FROM convite", Timestamp.class));

		assertFalse(conviteService.validar(token).valido());
		assertThrows(RegraDeNegocioException.class,
				() -> conviteService.cadastrar(cadastro(token, "52998224725", "outra.pessoa")));
		assertEquals(1, profissionalRepository.count());
	}

	/**
	 * Erro de digitacao nao pode queimar o convite.
	 *
	 * O consumo e marcado ANTES da checagem de CPF -- e o banco que decide
	 * quem ganhou a corrida pelo convite. Entao, quando o CPF ja existe, a
	 * marcacao ja aconteceu e precisa ser desfeita junto com o resto. Quem
	 * garante isso e o rollback da transacao, e rollback nao existe em
	 * teste com repositorio simulado.
	 */
	@Test
	void falhaNoCadastroDesfazTudoENaoQueimaOConvite() {
		profissionalRepository.save(profissionalExistente("12345678909"));
		String token = conviteService.emitir("joao@exemplo.com", "diretora").token();

		assertThrows(CpfJaCadastradoException.class,
				() -> conviteService.cadastrar(cadastro(token, "12345678909", "joao.pereira")));

		assertNull(jdbc.queryForObject("SELECT data_consumo FROM convite", Timestamp.class));
		assertTrue(conviteService.validar(token).valido());
		assertEquals(0, contar("SELECT count(*) FROM usuario"));

		// Corrigido o CPF, o mesmo convite funciona.
		conviteService.cadastrar(cadastro(token, "98765432100", "joao.pereira"));
		assertEquals(2, profissionalRepository.count());
	}

	/**
	 * Duplo clique, requisicao repetida: o mesmo token chegando duas vezes
	 * ao mesmo tempo.
	 *
	 * Quem garante a exclusividade e o UPDATE condicional
	 * (WHERE data_consumo IS NULL): a segunda transacao espera o bloqueio
	 * da linha, e quando a primeira confirma, afeta zero linhas.
	 *
	 * O teste nao consegue forcar que as duas threads se sobreponham de
	 * fato -- a ordem fica com o escalonador. Mas o que ele afirma vale em
	 * qualquer ordem: uma tentativa so da certo e existe um cadastro so.
	 */
	@Test
	void duasTentativasSimultaneasComOMesmoConviteCriamUmCadastroSo() throws Exception {
		String token = conviteService.emitir("ana@exemplo.com", "diretora").token();
		CountDownLatch largada = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<Boolean> primeira = executor.submit(
					tentativa(largada, cadastro(token, "39053344705", "ana.primeira")));
			Future<Boolean> segunda = executor.submit(
					tentativa(largada, cadastro(token, "16899535009", "ana.segunda")));

			largada.countDown();
			boolean primeiraDeuCerto = primeira.get(30, TimeUnit.SECONDS);
			boolean segundaDeuCerto = segunda.get(30, TimeUnit.SECONDS);

			assertTrue(primeiraDeuCerto ^ segundaDeuCerto, "exatamente uma tentativa deveria dar certo");
		} finally {
			executor.shutdownNow();
		}

		assertEquals(1, profissionalRepository.count());
		assertEquals(1, contar("SELECT count(*) FROM usuario"));
	}

	/**
	 * Devolve se o cadastro deu certo. Perder a corrida aparece como uma
	 * de duas excecoes, conforme o momento: convite ja consumido no banco
	 * (Conflito) ou ja invalido na leitura (RegraDeNegocio). Qualquer outra
	 * excecao nao e esperada e derruba o teste.
	 */
	private Callable<Boolean> tentativa(CountDownLatch largada, CadastroPorConviteDTO dados) {
		return () -> {
			largada.await();
			try {
				conviteService.cadastrar(dados);
				return true;
			} catch (ConflitoException | RegraDeNegocioException perdeu) {
				return false;
			}
		};
	}

	private CadastroPorConviteDTO cadastro(String token, String cpf, String login) {
		return new CadastroPorConviteDTO(
				token, "Pessoa de Teste", cpf, LocalDate.of(1990, 5, 20), login, "senha-de-teste-123");
	}

	private Profissional profissionalExistente(String cpf) {
		Profissional profissional = new Profissional();
		profissional.setNome("Cadastro Anterior");
		profissional.setCpf(cpf);
		profissional.setDataNascimento(LocalDate.of(1985, 1, 10));
		profissional.setDataAssociacao(LocalDate.of(2020, 3, 1));
		profissional.setAtivo(true);
		return profissional;
	}

}
