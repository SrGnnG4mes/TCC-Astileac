package br.edu.ifac.astileac.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARIO. Libera todos os endpoints sem autenticacao enquanto o
 * RF09 (autenticacao e perfis, ligada a tabela USUARIO) nao e
 * implementado -- Passo 6 do roteiro em CLAUDE.md.
 *
 * Sem essa classe, o Spring Security protege tudo com o usuario/senha
 * gerados automaticamente no boot, o que inviabiliza testar os
 * endpoints no Swagger durante o desenvolvimento.
 *
 * REMOVER/SUBSTITUIR quando a autenticacao real for implementada.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable());
		return http.build();
	}

}
