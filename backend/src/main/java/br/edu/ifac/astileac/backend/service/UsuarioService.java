package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.UsuarioResponseDTO;
import br.edu.ifac.astileac.backend.entity.Usuario;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.security.Perfil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contas de acesso (RF12).
 *
 * A criacao nao esta aqui: contas nascem pelo convite, no
 * ConviteService. Aqui ficam as duas operacoes sobre conta existente --
 * mudar o perfil e trocar a propria senha.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public List<UsuarioResponseDTO> listar() {
		return usuarioRepository.findAll().stream().map(this::paraResposta).toList();
	}

	/**
	 * Concede ou retira o perfil administrativo.
	 *
	 * A diretoria descreveu o perfil como status da conta, e e assim que
	 * esta implementado: a mesma conta muda de atributo, em vez de
	 * existir uma conta administrativa separada. Isso importa porque um
	 * diretor TAMBEM e associado -- ele precisa administrar o quadro e
	 * exercer os proprios direitos de titular sem trocar de login.
	 */
	@Transactional
	public UsuarioResponseDTO alterarPerfil(Long id, String perfilDesejado) {
		Perfil novo;
		try {
			novo = Perfil.valueOf(perfilDesejado.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new RegraDeNegocioException("Perfil invalido: use ADMIN ou PROFISSIONAL");
		}

		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conta nao encontrada: id " + id));

		boolean eraAdmin = Perfil.ADMIN.name().equals(usuario.getPerfil());
		boolean deixaDeSerAdmin = eraAdmin && novo != Perfil.ADMIN;

		// Sem esta guarda a associacao se tranca para fora do proprio
		// sistema: so administrador mexe em conta de administrador, e
		// recuperar exigiria alterar o banco na mao.
		if (deixaDeSerAdmin && usuarioRepository.countByPerfil(Perfil.ADMIN.name()) <= 1) {
			throw new ConflitoException(
					"Esta e a unica conta administradora. Conceda o perfil a outra conta antes de retirar desta.");
		}

		usuario.setPerfil(novo.name());
		Usuario salvo = usuarioRepository.save(usuario);

		log.info("Perfil da conta {} alterado para {}", id, novo);
		return paraResposta(salvo);
	}

	/**
	 * Troca a propria senha.
	 *
	 * Exige a senha atual: sem isso, uma sessao sequestrada trocaria a
	 * senha e trancaria o dono para fora da conta. A mensagem de falha
	 * nao distingue "senha errada" de qualquer outra causa pelo mesmo
	 * motivo de sempre.
	 */
	@Transactional
	public void trocarPropriaSenha(String login, String senhaAtual, String senhaNova) {
		Usuario usuario = usuarioRepository.findByLogin(login)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conta nao encontrada"));

		if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
			throw new RegraDeNegocioException("Senha atual incorreta");
		}

		if (passwordEncoder.matches(senhaNova, usuario.getSenhaHash())) {
			throw new RegraDeNegocioException("A senha nova deve ser diferente da atual");
		}

		usuario.setSenhaHash(passwordEncoder.encode(senhaNova));
		usuarioRepository.save(usuario);

		// Nunca a senha, obviamente -- so que houve troca.
		log.info("Senha trocada para a conta {}", usuario.getId());
	}

	private UsuarioResponseDTO paraResposta(Usuario usuario) {
		return new UsuarioResponseDTO(
				usuario.getId(),
				usuario.getLogin(),
				usuario.getPerfil(),
				usuario.getProfissional() != null ? usuario.getProfissional().getId() : null,
				usuario.getProfissional() != null ? usuario.getProfissional().getNome() : null);
	}
}
