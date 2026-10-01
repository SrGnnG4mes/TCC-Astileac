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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.CadastroPorConviteDTO;
import br.edu.ifac.astileac.backend.dto.ConviteEmitidoDTO;
import br.edu.ifac.astileac.backend.dto.ConviteValidacaoDTO;
import br.edu.ifac.astileac.backend.entity.Contato;
import br.edu.ifac.astileac.backend.entity.Convite;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.TipoContato;
import br.edu.ifac.astileac.backend.entity.Usuario;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.CpfJaCadastradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.ContatoRepository;
import br.edu.ifac.astileac.backend.repository.ConviteRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.security.Perfil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Teste de unidade do ConviteService (RF12).
 *
 * O ponto central e que o convite e o que liga "quem esta se
 * cadastrando" a "quem a associacao reconheceu como associado". Entao
 * os testes se concentram em nao deixar nada passar sem convite valido,
 * e em nao deixar um convite servir duas vezes.
 */
@ExtendWith(MockitoExtension.class)
class ConviteServiceTest {

	private static final String CPF_VALIDO = "11144477735";

	@Mock private ConviteRepository conviteRepository;
	@Mock private ProfissionalRepository profissionalRepository;
	@Mock private UsuarioRepository usuarioRepository;
	@Mock private ContatoRepository contatoRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private LogAcessoService logAcessoService;

	@InjectMocks private ConviteService conviteService;

	// ---------- emissao ----------

	@Test
	void emitir_devolveTokenEmTextoUmaVezEGuardaSomenteOHash() {
		when(conviteRepository.save(any(Convite.class))).thenAnswer(chamada -> {
			Convite c = chamada.getArgument(0);
			c.setId(1L);
			return c;
		});

		ConviteEmitidoDTO emitido = conviteService.emitir("Maria@Exemplo.COM", "diretora");

		assertNotNull(emitido.token());
		assertFalse(emitido.token().isBlank());

		ArgumentCaptor<Convite> capturado = ArgumentCaptor.forClass(Convite.class);
		verify(conviteRepository).save(capturado.capture());
		Convite salvo = capturado.getValue();

		// O que vai ao banco NAO pode ser o token.
		assertEquals(64, salvo.getTokenHash().length(), "o hash guardado deve ser SHA-256 em hexadecimal");
		assertFalse(salvo.getTokenHash().equals(emitido.token()));
		assertFalse(salvo.getTokenHash().contains(emitido.token()));

		// E-mail normalizado, para o mesmo endereco nao virar dois.
		assertEquals("maria@exemplo.com", salvo.getEmail());
		assertEquals("diretora", salvo.getEmitidoPor());
		assertTrue(salvo.getDataExpiracao().isAfter(LocalDateTime.now()));
	}

	@Test
	void emitir_doisConvitesGeramTokensDiferentes() {
		when(conviteRepository.save(any(Convite.class))).thenAnswer(c -> c.getArgument(0));

		String primeiro = conviteService.emitir("a@exemplo.com", "diretora").token();
		String segundo = conviteService.emitir("b@exemplo.com", "diretora").token();

		assertFalse(primeiro.equals(segundo));
	}

	// ---------- validacao ----------

	@Test
	void validar_conviteValido_devolveOEmailDeDestino() {
		when(conviteRepository.findByTokenHash(anyString()))
				.thenReturn(Optional.of(convitePendente()));

		ConviteValidacaoDTO resultado = conviteService.validar("qualquer-token");

		assertTrue(resultado.valido());
		assertEquals("maria@exemplo.com", resultado.email());
	}

	@Test
	void validar_tokenInexistente_naoRevelaQueNaoExiste() {
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		ConviteValidacaoDTO inexistente = conviteService.validar("inventado");

		Convite consumido = convitePendente();
		consumido.setDataConsumo(LocalDateTime.now().minusDays(1));
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(consumido));
		ConviteValidacaoDTO jaUsado = conviteService.validar("usado");

		assertFalse(inexistente.valido());
		assertFalse(jaUsado.valido());
		assertNull(inexistente.email());
		assertNull(jaUsado.email());
		// A resposta tem de ser indistinguivel: dizer "ja foi usado"
		// confirmaria a um estranho que aquele codigo existe.
		assertEquals(inexistente.motivo(), jaUsado.motivo());
	}

	@Test
	void validar_conviteExpirado_naoVale() {
		Convite expirado = convitePendente();
		expirado.setDataExpiracao(LocalDateTime.now().minusMinutes(1));
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expirado));

		assertFalse(conviteService.validar("token").valido());
	}

	// ---------- cadastro ----------

	@Test
	void cadastrar_criaProfissionalUsuarioEContatoEConsomeOConvite() {
		Convite convite = convitePendente();
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(convite));
		when(conviteRepository.marcarConsumido(eq(1L), any(LocalDateTime.class))).thenReturn(1);
		when(profissionalRepository.existsByCpf(CPF_VALIDO)).thenReturn(false);
		when(usuarioRepository.existsByLogin("maria")).thenReturn(false);
		when(passwordEncoder.encode("senha-secreta")).thenReturn("$2a$hash");
		when(profissionalRepository.save(any(Profissional.class))).thenAnswer(chamada -> {
			Profissional p = chamada.getArgument(0);
			p.setId(77L);
			return p;
		});

		Long id = conviteService.cadastrar(dadosDeCadastro());

		assertEquals(77L, id);

		ArgumentCaptor<Profissional> profissional = ArgumentCaptor.forClass(Profissional.class);
		verify(profissionalRepository).save(profissional.capture());
		assertEquals("Maria Silva", profissional.getValue().getNome());
		assertTrue(profissional.getValue().getAtivo());
		// Quem define a data de associacao e a associacao, nao o
		// formulario: e o dia em que o convite foi usado.
		assertEquals(LocalDate.now(), profissional.getValue().getDataAssociacao());

		ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(usuario.capture());
		assertEquals("$2a$hash", usuario.getValue().getSenhaHash());
		// Ninguem se promove a administrador preenchendo formulario.
		assertEquals(Perfil.PROFISSIONAL.name(), usuario.getValue().getPerfil());

		// O convite gravado no fim tem de estar marcado como consumido.
		//
		// Parece redundante com o marcarConsumido, e nao e: aquele e um
		// UPDATE em massa, que altera a linha no banco mas nao a
		// entidade carregada. Sem sincronizar a entidade, este save()
		// grava o estado obsoleto por cima e o convite volta a valer --
		// foi o que aconteceu de verdade, e so apareceu rodando contra
		// o banco.
		ArgumentCaptor<Convite> conviteSalvo = ArgumentCaptor.forClass(Convite.class);
		verify(conviteRepository).save(conviteSalvo.capture());
		assertNotNull(conviteSalvo.getValue().getDataConsumo(),
				"o convite gravado nao pode voltar ao banco sem data de consumo");
		assertEquals(77L, conviteSalvo.getValue().getProfissional().getId());

		ArgumentCaptor<Contato> contato = ArgumentCaptor.forClass(Contato.class);
		verify(contatoRepository).save(contato.capture());
		assertEquals(TipoContato.EMAIL, contato.getValue().getTipo());
		// O e-mail vem do CONVITE, nao do formulario: e o endereco que
		// a associacao reconhece, nao o que a pessoa digitou agora.
		assertEquals("maria@exemplo.com", contato.getValue().getValor());
		assertTrue(contato.getValue().getPrincipal());
	}

	@Test
	void cadastrar_semConviteValido_naoCriaNada() {
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThrows(RegraDeNegocioException.class, () -> conviteService.cadastrar(dadosDeCadastro()));

		verify(profissionalRepository, never()).save(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void cadastrar_conviteExpirado_naoCriaNada() {
		Convite expirado = convitePendente();
		expirado.setDataExpiracao(LocalDateTime.now().minusMinutes(1));
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expirado));

		assertThrows(RegraDeNegocioException.class, () -> conviteService.cadastrar(dadosDeCadastro()));

		verify(profissionalRepository, never()).save(any());
	}

	/**
	 * O convite e credencial portadora e pode chegar duas vezes ao mesmo
	 * tempo -- duplo clique, requisicao repetida. Quem decide o vencedor
	 * e o banco, pela atualizacao condicional: a segunda chamada afeta
	 * zero linhas e nao pode criar cadastro nenhum.
	 */
	@Test
	void cadastrar_quandoOutraRequisicaoConsumiuPrimeiro_naoCriaNada() {
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(convitePendente()));
		when(conviteRepository.marcarConsumido(anyLong(), any(LocalDateTime.class))).thenReturn(0);

		assertThrows(ConflitoException.class, () -> conviteService.cadastrar(dadosDeCadastro()));

		verify(profissionalRepository, never()).save(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void cadastrar_cpfJaCadastrado_recusa() {
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(convitePendente()));
		when(conviteRepository.marcarConsumido(anyLong(), any(LocalDateTime.class))).thenReturn(1);
		when(profissionalRepository.existsByCpf(CPF_VALIDO)).thenReturn(true);

		assertThrows(CpfJaCadastradoException.class, () -> conviteService.cadastrar(dadosDeCadastro()));

		verify(profissionalRepository, never()).save(any());
	}

	@Test
	void cadastrar_loginEmUso_recusa() {
		when(conviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(convitePendente()));
		when(conviteRepository.marcarConsumido(anyLong(), any(LocalDateTime.class))).thenReturn(1);
		when(profissionalRepository.existsByCpf(CPF_VALIDO)).thenReturn(false);
		when(usuarioRepository.existsByLogin("maria")).thenReturn(true);

		assertThrows(ConflitoException.class, () -> conviteService.cadastrar(dadosDeCadastro()));

		verify(profissionalRepository, never()).save(any());
		verify(usuarioRepository, never()).save(any());
	}

	// ---------- apoio ----------

	private Convite convitePendente() {
		Convite convite = new Convite();
		convite.setId(1L);
		convite.setEmail("maria@exemplo.com");
		convite.setTokenHash("hash-qualquer");
		convite.setDataEmissao(LocalDateTime.now().minusDays(1));
		convite.setDataExpiracao(LocalDateTime.now().plusDays(6));
		convite.setEmitidoPor("diretora");
		return convite;
	}

	private CadastroPorConviteDTO dadosDeCadastro() {
		return new CadastroPorConviteDTO(
				"token-em-texto",
				"Maria Silva",
				CPF_VALIDO,
				LocalDate.of(1990, 5, 10),
				"maria",
				"senha-secreta");
	}
}
