package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.DadosPessoaisDTO;
import br.edu.ifac.astileac.backend.dto.ExclusaoRespostaDTO;
import br.edu.ifac.astileac.backend.entity.AcaoLog;
import br.edu.ifac.astileac.backend.entity.Convite;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.Usuario;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.CertificadoRepository;
import br.edu.ifac.astileac.backend.repository.ContatoRepository;
import br.edu.ifac.astileac.backend.repository.ConviteRepository;
import br.edu.ifac.astileac.backend.repository.EnderecoRepository;
import br.edu.ifac.astileac.backend.repository.FormacaoRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalAreaRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.repository.VinculoEmpregaticioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Teste de unidade dos direitos do titular (RF10, LGPD Art. 18).
 *
 * O comportamento que mais importa aqui: pedir exclusao NAO apaga na
 * hora. Se algum dia alguem trocar isso por um delete direto, este
 * teste quebra.
 */
@ExtendWith(MockitoExtension.class)
class DadosPessoaisServiceTest {

	@Mock
	private ProfissionalRepository profissionalRepository;
	@Mock
	private EnderecoRepository enderecoRepository;
	@Mock
	private ContatoRepository contatoRepository;
	@Mock
	private ProfissionalAreaRepository profissionalAreaRepository;
	@Mock
	private FormacaoRepository formacaoRepository;
	@Mock
	private CertificadoRepository certificadoRepository;
	@Mock
	private VinculoEmpregaticioRepository vinculoEmpregaticioRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private ConviteRepository conviteRepository;
	@Mock
	private ProfissionalService profissionalService;
	@Mock
	private LogAcessoService logAcessoService;

	@Mock
	private ArquivoCertificadoService arquivoCertificadoService;

	@InjectMocks
	private DadosPessoaisService dadosPessoaisService;

	private Profissional profissional() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		profissional.setNome("Maria Silva");
		profissional.setCpf("11144477735");
		profissional.setDataNascimento(LocalDate.of(1990, 5, 10));
		profissional.setDataAssociacao(LocalDate.of(2024, 1, 15));
		profissional.setAtivo(true);
		return profissional;
	}

	@Test
	void pedidoDeExclusaoNaoDeveApagarNaHora() {
		Profissional profissional = profissional();
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional);
		when(profissionalRepository.save(any(Profissional.class))).thenAnswer(i -> i.getArgument(0));

		dadosPessoaisService.solicitarExclusao(1L);

		// Os dados continuam no banco, apenas bloqueados.
		verify(profissionalRepository, never()).delete(any());
		assertNotNull(profissional.getDataExclusaoSolicitada());
		assertFalse(profissional.getAtivo(), "o cadastro deve ficar bloqueado durante a janela");
	}

	@Test
	void aRespostaDeveInformarExplicitamenteOPrazoDe30Dias() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(profissionalRepository.save(any(Profissional.class))).thenAnswer(i -> i.getArgument(0));

		ExclusaoRespostaDTO resposta = dadosPessoaisService.solicitarExclusao(1L);

		assertEquals(30, resposta.diasParaArrependimento());
		assertEquals(LocalDate.now().plusDays(30), resposta.dataDaExclusaoDefinitiva());
		assertTrue(resposta.mensagem().contains("30 dias"),
				"a pessoa precisa ler o prazo, nao so receber uma data");
		assertTrue(resposta.mensagem().contains("mudar de ideia"),
				"a mensagem precisa dizer para que serve o prazo");
		assertNotNull(resposta.comoCancelar(), "a pessoa precisa saber como desfazer");
	}

	@Test
	void naoDeveAceitarDoisPedidosDeExclusaoParaOMesmoCadastro() {
		Profissional jaSolicitado = profissional();
		jaSolicitado.setDataExclusaoSolicitada(LocalDateTime.now().minusDays(3));
		when(profissionalService.buscarEntidade(1L)).thenReturn(jaSolicitado);

		assertThrows(ConflitoException.class, () -> dadosPessoaisService.solicitarExclusao(1L));
	}

	@Test
	void cancelarDeveDevolverOCadastroAoNormal() {
		Profissional emJanela = profissional();
		emJanela.setDataExclusaoSolicitada(LocalDateTime.now().minusDays(5));
		emJanela.setAtivo(false);
		when(profissionalService.buscarEntidade(1L)).thenReturn(emJanela);
		when(profissionalRepository.save(any(Profissional.class))).thenAnswer(i -> i.getArgument(0));

		dadosPessoaisService.cancelarExclusao(1L);

		assertNull(emJanela.getDataExclusaoSolicitada());
		assertTrue(emJanela.getAtivo());
	}

	@Test
	void naoDeveCancelarQuandoNaoHaPedido() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());

		assertThrows(RecursoNaoEncontradoException.class, () -> dadosPessoaisService.cancelarExclusao(1L));
	}

	@Test
	void expurgoDeveApagarFilhosAntesDoProfissional() {
		Profissional vencido = profissional();
		vencido.setDataExclusaoSolicitada(LocalDateTime.now().minusDays(31));
		when(profissionalRepository.findByDataExclusaoSolicitadaBefore(any(LocalDateTime.class)))
				.thenReturn(List.of(vencido));
		when(certificadoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(formacaoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(vinculoEmpregaticioRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(profissionalAreaRepository.findByIdProfissionalId(1L)).thenReturn(List.of());
		when(contatoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(enderecoRepository.findByProfissionalId(1L)).thenReturn(Optional.empty());

		int apagados = dadosPessoaisService.expurgarPrazoVencido();

		assertEquals(1, apagados);
		verify(profissionalRepository).delete(vencido);
		// A eliminacao fica registrada, sem identificar a pessoa.
		verify(logAcessoService).registrar(eq(AcaoLog.EXCLUSAO), anyString(), eq(1L), any(), anyString());
	}

	/**
	 * Desde o RF12 um profissional pode ter conta de acesso, e a chave
	 * estrangeira de USUARIO nao tem ON DELETE CASCADE: apagar o
	 * profissional sem apagar a conta antes viola a restricao.
	 *
	 * Nao seria falha pequena. O laco do expurgo inteiro roda numa
	 * transacao so, as 3h da manha: a violacao reverteria a eliminacao
	 * de TODOS os cadastros vencidos, deixando uma linha de log como
	 * unica pista. O sistema pararia de cumprir o Art. 18 em silencio.
	 */
	@Test
	void expurgoDeveApagarAContaDeAcessoAntesDoProfissional() {
		Profissional vencido = profissional();
		vencido.setDataExclusaoSolicitada(LocalDateTime.now().minusDays(31));

		Usuario conta = new Usuario();
		conta.setId(9L);
		conta.setLogin("maria");

		Convite convite = new Convite();
		convite.setId(3L);

		when(profissionalRepository.findByDataExclusaoSolicitadaBefore(any(LocalDateTime.class)))
				.thenReturn(List.of(vencido));
		when(certificadoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(formacaoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(vinculoEmpregaticioRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(profissionalAreaRepository.findByIdProfissionalId(1L)).thenReturn(List.of());
		when(contatoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(enderecoRepository.findByProfissionalId(1L)).thenReturn(Optional.empty());
		when(usuarioRepository.findByProfissionalId(1L)).thenReturn(Optional.of(conta));
		when(conviteRepository.findByProfissionalId(1L)).thenReturn(Optional.of(convite));

		dadosPessoaisService.expurgarPrazoVencido();

		// A ordem e o que importa: conta e convite antes do profissional.
		InOrder ordem = inOrder(conviteRepository, usuarioRepository, profissionalRepository);
		ordem.verify(conviteRepository).delete(convite);
		ordem.verify(usuarioRepository).delete(conta);
		ordem.verify(profissionalRepository).delete(vencido);
	}

	@Test
	void exportacaoDeveTrazerOCpfCompletoEFicarRegistrada() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(enderecoRepository.findByProfissionalId(1L)).thenReturn(Optional.empty());
		when(contatoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(profissionalAreaRepository.findByIdProfissionalId(1L)).thenReturn(List.of());
		when(formacaoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(certificadoRepository.findByProfissionalId(1L)).thenReturn(List.of());
		when(vinculoEmpregaticioRepository.findByProfissionalId(1L)).thenReturn(List.of());

		DadosPessoaisDTO resultado = dadosPessoaisService.exportar(1L);

		// O destinatario e o proprio titular: mascarar o CPF dele para
		// ele mesmo nao protegeria ninguem.
		assertEquals("11144477735", resultado.identificacao().cpf());
		// Entregar a ficha inteira e evento distinto de abrir uma tela.
		verify(logAcessoService).registrar(eq(AcaoLog.EXPORTACAO), anyString(), eq(1L), any(), anyString());
	}

}
