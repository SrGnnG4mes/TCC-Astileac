package br.edu.ifac.astileac.backend.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acesso ao usuario da requisicao atual, em um lugar so.
 *
 * Registrado como bean com o nome "autorizacao" para poder ser chamado
 * de dentro de @PreAuthorize -- ver os controllers.
 */
@Component("autorizacao")
public class UsuarioLogado {

	public Optional<UsuarioAutenticado> atual() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null || !autenticacao.isAuthenticated()
				|| !(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) {
			return Optional.empty();
		}
		return Optional.of(usuario);
	}

	/** Login de quem esta logado, para o registro de acesso (RF11). */
	public String loginAtual() {
		return atual().map(UsuarioAutenticado::getLogin).orElse("nao-autenticado");
	}

	public boolean eAdmin() {
		return atual().map(usuario -> usuario.getPerfil() == Perfil.ADMIN).orElse(false);
	}

	/**
	/**
	 * Usado em @PreAuthorize, COMPOSTO com hasRole: a expressao fica
	 * "hasRole(ADMIN) or @autorizacao.eOProprioProfissional(#id)".
	 *
	 * Este metodo cuida so da propriedade do registro. A checagem de
	 * perfil fica com o mecanismo padrao do Spring -- alem de mais
	 * legivel na anotacao, torna a regra testavel com @WithMockUser,
	 * o que nao seria possivel se o perfil fosse verificado aqui
	 * dentro pelo tipo do principal.
	 */
	public boolean eOProprioProfissional(Long profissionalId) {
		return atual()
				.map(usuario -> profissionalId.equals(usuario.getProfissionalId()))
				.orElse(false);
	}

}
