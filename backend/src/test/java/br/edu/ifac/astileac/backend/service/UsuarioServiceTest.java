package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.UsuarioResponseDTO;
import br.edu.ifac.astileac.backend.entity.Usuario;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.security.Perfil;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Teste de unidade das contas de acesso (RF12). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock private UsuarioRepository usuarioRepository;
	@Mock private PasswordEncoder passwordEncoder;

	@InjectMocks private UsuarioService usuarioService;

	// ---------- perfil ----------

	@Test
	void alterarPerfil_concedeAdministradorAUmAssociado() {
		Usuario associado = conta(5L, "maria", Perfil.PROFISSIONAL);
		when(usuarioRepository.findById(5L)).thenReturn(Optional.of(associado));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(c -> c.getArgument(0));

		UsuarioResponseDTO resultado = usuarioService.alterarPerfil(5L, "ADMIN");

		assertEquals(Perfil.ADMIN.name(), resultado.perfil());
	}

	/**
	 * Sem esta guarda a associacao se tranca para fora do proprio
	 * sistema: so administrador mexe em conta de administrador, e sair
	 * disso exigiria alterar o banco na mao.
	 */
	@Test
	void alterarPerfil_naoRetiraOPerfilDoUltimoAdministrador() {
		Usuario unicoAdmin = conta(1L, "diretora", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(unicoAdmin));
		when(usuarioRepository.countByPerfil(Perfil.ADMIN.name())).thenReturn(1L);

		assertThrows(ConflitoException.class,
				() -> usuarioService.alterarPerfil(1L, "PROFISSIONAL"));

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void alterarPerfil_retiraOPerfilQuandoHaOutroAdministrador() {
		Usuario admin = conta(1L, "diretora", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(usuarioRepository.countByPerfil(Perfil.ADMIN.name())).thenReturn(2L);
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(c -> c.getArgument(0));

		UsuarioResponseDTO resultado = usuarioService.alterarPerfil(1L, "PROFISSIONAL");

		assertEquals(Perfil.PROFISSIONAL.name(), resultado.perfil());
	}

	@Test
	void alterarPerfil_perfilInexistente_recusa() {
		assertThrows(RegraDeNegocioException.class, () -> usuarioService.alterarPerfil(1L, "SUPERUSUARIO"));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void alterarPerfil_contaInexistente_404() {
		when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
		assertThrows(RecursoNaoEncontradoException.class, () -> usuarioService.alterarPerfil(99L, "ADMIN"));
	}

	// ---------- senha ----------

	@Test
	void trocarPropriaSenha_gravaOHashDaNova() {
		Usuario usuario = conta(5L, "maria", Perfil.PROFISSIONAL);
		usuario.setSenhaHash("$2a$antiga");
		when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("atual", "$2a$antiga")).thenReturn(true);
		when(passwordEncoder.matches("nova-senha", "$2a$antiga")).thenReturn(false);
		when(passwordEncoder.encode("nova-senha")).thenReturn("$2a$nova");

		usuarioService.trocarPropriaSenha("maria", "atual", "nova-senha");

		ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(salvo.capture());
		assertEquals("$2a$nova", salvo.getValue().getSenhaHash());
	}

	/**
	 * Sem exigir a senha atual, uma sessao sequestrada trocaria a senha
	 * e trancaria o dono para fora da propria conta.
	 */
	@Test
	void trocarPropriaSenha_senhaAtualErrada_recusa() {
		Usuario usuario = conta(5L, "maria", Perfil.PROFISSIONAL);
		usuario.setSenhaHash("$2a$antiga");
		when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("chute", "$2a$antiga")).thenReturn(false);

		assertThrows(RegraDeNegocioException.class,
				() -> usuarioService.trocarPropriaSenha("maria", "chute", "nova-senha"));

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void trocarPropriaSenha_repetindoAAtual_recusa() {
		Usuario usuario = conta(5L, "maria", Perfil.PROFISSIONAL);
		usuario.setSenhaHash("$2a$antiga");
		when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches(anyString(), any())).thenReturn(true);

		assertThrows(RegraDeNegocioException.class,
				() -> usuarioService.trocarPropriaSenha("maria", "atual", "atual"));

		verify(usuarioRepository, never()).save(any());
	}

	private Usuario conta(Long id, String login, Perfil perfil) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setLogin(login);
		usuario.setPerfil(perfil.name());
		usuario.setSenhaHash("$2a$hash");
		return usuario;
	}
}
