package br.edu.ifac.astileac.backend.security;

import br.edu.ifac.astileac.backend.entity.Usuario;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Quem esta logado, do ponto de vista do Spring Security.
 *
 * Carrega o profissionalId junto: a regra "so os proprios dados"
 * precisa comparar esse id a cada requisicao, e guarda-lo aqui evita
 * ir ao banco toda vez. Nulo significa conta administrativa que nao
 * corresponde a um associado.
 */
@Getter
public class UsuarioAutenticado implements UserDetails {

	private final Long usuarioId;
	private final Long profissionalId;
	private final String login;
	private final String senhaHash;
	private final Perfil perfil;

	public UsuarioAutenticado(Usuario usuario) {
		this.usuarioId = usuario.getId();
		this.profissionalId = usuario.getProfissional() != null ? usuario.getProfissional().getId() : null;
		this.login = usuario.getLogin();
		this.senhaHash = usuario.getSenhaHash();
		this.perfil = Perfil.valueOf(usuario.getPerfil());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(perfil.comoAutoridade()));
	}

	@Override
	public String getPassword() {
		return senhaHash;
	}

	@Override
	public String getUsername() {
		return login;
	}

}
