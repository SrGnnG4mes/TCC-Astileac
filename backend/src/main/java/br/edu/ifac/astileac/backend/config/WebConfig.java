package br.edu.ifac.astileac.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracao da camada web.
 *
 * PageSerializationMode.VIA_DTO fixa o formato JSON das respostas
 * paginadas: por padrao o Spring serializa o PageImpl interno e a
 * propria biblioteca avisa no log que a estrutura nao tem estabilidade
 * garantida entre versoes. Como o front-end Angular (Passo 7) vai
 * depender desse formato, VIA_DTO troca por um contrato estavel.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final LogAcessoInterceptor logAcessoInterceptor;

	/**
	 * O registro de acesso (RF11) cobre so as rotas que tocam dado
	 * pessoal. Swagger, api-docs e o /hello ficam de fora: registrar
	 * acesso a documentacao da API nao ajuda ninguem e so polui a
	 * auditoria.
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(logAcessoInterceptor)
				.addPathPatterns("/profissionais/**", "/areas-atuacao/**");
	}

}
