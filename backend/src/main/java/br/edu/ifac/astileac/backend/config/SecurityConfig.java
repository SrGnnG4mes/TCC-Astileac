package br.edu.ifac.astileac.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Autenticacao e controle de acesso por perfil (RF09).
 *
 * Substitui o SecurityConfig temporario que liberava tudo.
 *
 * SESSAO COM COOKIE, nao JWT. Motivo: logout com JWT exige uma lista
 * de revogacao no servidor -- ou seja, reintroduz o estado que o
 * "stateless" prometia evitar. O cookie de sessao e HttpOnly (o
 * JavaScript nao le, entao um XSS nao rouba a sessao), enquanto um
 * token em localStorage seria legivel. Escala horizontal sem estado
 * compartilhado, o problema que o JWT resolve, nao existe aqui: e um
 * sistema interno com um servidor so.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	/**
	 * BCrypt, como manda a regra do projeto. O custo padrao (10) e
	 * proposital: aumenta-lo torna a verificacao mais lenta tanto para
	 * um atacante quanto para o login legitimo.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf
						// O Angular le este cookie e devolve o valor no
						// cabecalho X-XSRF-TOKEN. Por isso NAO e HttpOnly:
						// e o unico cookie que o front-end precisa ler.
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						// A partir do Security 6 o token e resolvido de forma
						// preguicosa. Este handler restaura a resolucao direta,
						// que e o que o padrao de SPA espera.
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.sessionManagement(sessao -> sessao
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						// Uma sessao por usuario: login novo derruba o anterior.
						.maximumSessions(1))
				.authorizeHttpRequests(auth -> auth
						// Login precisa ser alcancavel por quem ainda nao entrou.
						// O token CSRF precisa ser alcancavel antes do login:
						// sem ele o proprio POST de login seria bloqueado.
						.requestMatchers(HttpMethod.GET, "/autenticacao/csrf").permitAll()
						// Interface de teste, servida pelo proprio backend. Fica na
						// mesma origem da API, o que dispensa CORS: o navegador
						// manda o cookie de sessao sem configuracao extra.
						.requestMatchers(HttpMethod.GET, "/", "/index.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/autenticacao/login").permitAll()
						// Cadastro por convite (RF12). Publico por
						// necessidade: quem usa o convite ainda nao tem
						// conta. O que faz as vezes de autenticacao e o
						// proprio token -- emitido pela diretoria depois do
						// pagamento, de uso unico e com prazo. Sem token
						// valido, nenhum dos dois caminhos cria nada.
						.requestMatchers(HttpMethod.POST, "/cadastro", "/cadastro/validacao").permitAll()
						// Documentacao da API. Desligar no ambiente de destino
						// (ver application.properties).
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
						// Cadastrar e consultar o quadro de associados e
						// atribuicao da diretoria. O associado nao navega pelo
						// cadastro dos outros.
						.requestMatchers(HttpMethod.POST, "/profissionais").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/profissionais").hasRole("ADMIN")
						// Manter a tabela de dominio tambem e da diretoria.
						.requestMatchers(HttpMethod.POST, "/areas-atuacao").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/areas-atuacao/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/areas-atuacao/**").hasRole("ADMIN")
						// O resto exige login; quem alcanca o que e decidido
						// por @PreAuthorize nos controllers, porque depende de
						// quem e o dono do registro.
						.anyRequest().authenticated())
				.formLogin(login -> login
						.loginProcessingUrl("/autenticacao/login")
						// Nomes do dominio, nao os padroes do Spring.
						.usernameParameter("login")
						.passwordParameter("senha")
						// Respostas em JSON: o cliente e uma SPA, nao um
						// navegador seguindo redirecionamento.
						.successHandler((req, resp, auth) -> resp.setStatus(HttpServletResponse.SC_OK))
						.failureHandler((req, resp, ex) -> {
							resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
							resp.getWriter().write("{\"status\":401,\"mensagem\":\"Credenciais invalidas\"}");
						}))
				.logout(logout -> logout
						.logoutUrl("/autenticacao/logout")
						.deleteCookies("JSESSIONID")
						.logoutSuccessHandler((req, resp, auth) -> resp.setStatus(HttpServletResponse.SC_OK)))
				.exceptionHandling(erros -> erros
						// Sem isto, o Spring redirecionaria para uma pagina de
						// login que nao existe. Uma API responde com status.
						.authenticationEntryPoint((req, resp, ex) -> {
							resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
							resp.getWriter().write("{\"status\":401,\"mensagem\":\"Autenticacao necessaria\"}");
						})
						// Duas causas muito diferentes chegam aqui como o mesmo
						// 403: token CSRF ausente/invalido e falta de
						// permissao. Tratar as duas com a mesma mensagem torna
						// o erro indiagnosticavel -- quem recebe nao sabe se
						// deve renovar o token ou se simplesmente nao pode
						// fazer aquilo. Distinguir nao vaza nada: nao revela
						// dado nem estrutura interna, so diz o que corrigir.
						.accessDeniedHandler((req, resp, ex) -> {
							boolean falhaDeCsrf = ex instanceof CsrfException;
							resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
							resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
							resp.getWriter().write(falhaDeCsrf
									? "{\"status\":403,\"mensagem\":\"Token CSRF ausente ou invalido. "
											+ "Obtenha um novo em GET /autenticacao/csrf e envie no cabecalho "
											+ "X-XSRF-TOKEN.\"}"
									: "{\"status\":403,\"mensagem\":\"Acesso negado: seu perfil nao permite "
											+ "esta operacao\"}");
						}));

		return http.build();
	}

	/**
	 * O Angular roda em outra origem (porta 4200) durante o
	 * desenvolvimento. allowCredentials e obrigatorio para o cookie de
	 * sessao trafegar, e por isso a origem tem que ser declarada
	 * explicitamente -- o curinga "*" e recusado junto com
	 * credenciais, justamente para nao expor a sessao a qualquer site.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuracao = new CorsConfiguration();
		configuracao.setAllowedOrigins(List.of("http://localhost:4200"));
		configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuracao.setAllowedHeaders(List.of("*"));
		configuracao.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
		fonte.registerCorsConfiguration("/**", configuracao);
		return fonte;
	}

}
