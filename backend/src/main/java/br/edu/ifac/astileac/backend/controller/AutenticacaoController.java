package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.UsuarioAutenticadoDTO;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.security.UsuarioAutenticado;
import br.edu.ifac.astileac.backend.security.UsuarioLogado;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sessao do usuario (RF09).
 *
 * Login e logout NAO aparecem aqui: sao tratados pelo proprio Spring
 * Security em POST /autenticacao/login e POST /autenticacao/logout
 * (ver SecurityConfig). Reimplementa-los a mao significaria cuidar de
 * criacao de sessao e gravacao do contexto de seguranca por conta
 * propria, que e exatamente o que o framework ja faz certo.
 */
@RestController
@RequestMapping("/autenticacao")
@RequiredArgsConstructor
public class AutenticacaoController {

	private final UsuarioLogado usuarioLogado;

	/**
	 * Entrega o token CSRF e, no mesmo movimento, faz o cookie ser
	 * escrito.
	 *
	 * Desde o Security 6 o token e resolvido de forma preguicosa: o
	 * cookie so aparece quando alguem LE o token. Sem um ponto que
	 * faca essa leitura, a SPA nunca receberia o cookie e o proprio
	 * login seria bloqueado por falta de token -- um impasse. Ler
	 * csrfToken.getToken() aqui e o que quebra esse ciclo.
	 *
	 * O front-end chama isto uma vez, antes do login.
	 */
	@GetMapping("/csrf")
	public Map<String, String> csrf(CsrfToken csrfToken) {
		return Map.of(
				"token", csrfToken.getToken(),
				"headerName", csrfToken.getHeaderName());
	}

	/**
	 * Quem sou eu. O front-end chama isto ao carregar para saber se a
	 * sessao continua valida e qual perfil renderizar.
	 */
	@GetMapping("/eu")
	public UsuarioAutenticadoDTO eu() {
		UsuarioAutenticado usuario = usuarioLogado.atual()
				.orElseThrow(() -> new RecursoNaoEncontradoException("Nenhuma sessão ativa."));

		return new UsuarioAutenticadoDTO(
				usuario.getLogin(),
				usuario.getPerfil(),
				usuario.getProfissionalId());
	}

}
