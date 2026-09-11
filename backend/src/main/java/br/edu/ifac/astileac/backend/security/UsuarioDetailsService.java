package br.edu.ifac.astileac.backend.security;

import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liga o Spring Security a tabela USUARIO (RF09).
 *
 * A mensagem de erro nao distingue "login inexistente" de "senha
 * errada": dizer qual dos dois falhou permitiria descobrir quais
 * logins existem testando um por um.
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String login) {
		return usuarioRepository.findByLogin(login)
				.map(UsuarioAutenticado::new)
				.orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));
	}

}
