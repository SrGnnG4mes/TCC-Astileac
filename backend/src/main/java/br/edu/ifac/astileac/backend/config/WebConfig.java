package br.edu.ifac.astileac.backend.config;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configuracao da camada web.
 *
 * PageSerializationMode.VIA_DTO fixa o formato JSON das respostas
 * paginadas: por padrao o Spring serializa o PageImpl interno e a
 * propria biblioteca avisa no log que a estrutura nao tem estabilidade
 * garantida entre versoes. Como o front-end Angular depende desse
 * formato, VIA_DTO troca por um contrato estavel.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	/**
	 * Prefixo de toda a API.
	 *
	 * O back-end serve tambem o Angular compilado, e as rotas do Angular
	 * tem os mesmos nomes dos recursos da API: /profissionais,
	 * /convites, /areas-atuacao. Sem um prefixo, abrir
	 * /profissionais no navegador devolveria o JSON da API em vez da
	 * aplicacao -- o mesmo defeito de link direto que ja tinha aparecido
	 * no proxy do servidor de desenvolvimento. Com o prefixo, "/api/..."
	 * e API e o resto e aplicacao, no desenvolvimento e na implantacao.
	 */
	public static final String PREFIXO_API = "/api";

	private static final String PACOTE_DOS_CONTROLLERS = "br.edu.ifac.astileac.backend.controller";

	private final LogAcessoInterceptor logAcessoInterceptor;

	/**
	 * Aplica o prefixo so aos controllers do sistema, escolhidos pelo
	 * pacote. Os do springdoc (Swagger) ficam onde a documentacao deles
	 * diz que estao.
	 */
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(PREFIXO_API, HandlerTypePredicate.forBasePackage(PACOTE_DOS_CONTROLLERS));
	}

	/**
	 * O registro de acesso (RF11) cobre so as rotas que tocam dado
	 * pessoal. Swagger e api-docs ficam de fora: registrar acesso a
	 * documentacao da API nao ajuda ninguem e so polui a auditoria.
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(logAcessoInterceptor)
				.addPathPatterns(PREFIXO_API + "/profissionais/**", PREFIXO_API + "/areas-atuacao/**");
	}

	/**
	 * Serve o Angular compilado, com volta para o index.html.
	 *
	 * O Angular decide a tela pelo caminho da URL, mas so depois de
	 * carregado. Quem recarrega /profissionais/3, ou abre o link do
	 * convite, pede ao servidor um arquivo que nao existe: sem esta volta
	 * receberia 404. Entao todo caminho que nao e arquivo devolve o
	 * index.html, e o Angular assume dali.
	 *
	 * Duas excecoes continuam dando 404 de verdade: o que esta sob /api
	 * (endpoint inexistente nao pode virar pagina HTML com status 200) e
	 * o que tem extensao (um .js que falta tem que aparecer como falta,
	 * nao como HTML no lugar de script).
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String caminho, Resource local) throws IOException {
						Resource pedido = local.createRelative(caminho);
						if (pedido.exists() && pedido.isReadable()) {
							return pedido;
						}

						String ultimoTrecho = caminho.substring(caminho.lastIndexOf('/') + 1);
						String api = PREFIXO_API.substring(1);
						if (caminho.equals(api) || caminho.startsWith(api + "/") || ultimoTrecho.contains(".")) {
							return null;
						}

						Resource aplicacao = local.createRelative("index.html");
						return aplicacao.exists() && aplicacao.isReadable() ? aplicacao : null;
					}
				});
	}

}
