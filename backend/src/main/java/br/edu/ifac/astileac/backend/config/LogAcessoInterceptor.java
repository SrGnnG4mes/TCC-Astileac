package br.edu.ifac.astileac.backend.config;

import br.edu.ifac.astileac.backend.entity.AcaoLog;
import br.edu.ifac.astileac.backend.service.LogAcessoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Registra no banco toda requisicao que toca dado pessoal (RF11).
 *
 * Fica no interceptor, e nao espalhado pelos services, por dois
 * motivos: nao ha como esquecer de registrar num endpoint novo, e a
 * regra de negocio nao fica misturada com auditoria. Em troca, o que
 * se registra e a operacao HTTP -- nao quais campos foram lidos.
 *
 * Registra apos a resposta, e somente quando ela foi bem-sucedida:
 * tentativa recusada por validacao nao e acesso a dado pessoal.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogAcessoInterceptor implements HandlerInterceptor {

	private final LogAcessoService logAcessoService;

	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

		if (response.getStatus() >= 400) {
			return;
		}

		try {
			String caminho = request.getRequestURI();
			String recurso = semPrefixo(caminho);
			logAcessoService.registrar(
					acaoDe(request.getMethod()),
					entidadeDe(recurso),
					registroIdDe(recurso),
					request.getRemoteAddr(),
					request.getMethod() + " " + caminho);
		} catch (RuntimeException falha) {
			// Auditoria nunca pode derrubar a operacao do usuario: a
			// resposta ja foi enviada. Registra no log da aplicacao
			// para que a falha nao passe despercebida.
			log.error("Falha ao gravar registro de acesso", falha);
		}
	}

	private AcaoLog acaoDe(String metodo) {
		return switch (metodo) {
			case "POST" -> AcaoLog.CRIACAO;
			case "PUT", "PATCH" -> AcaoLog.ALTERACAO;
			case "DELETE" -> AcaoLog.EXCLUSAO;
			default -> AcaoLog.CONSULTA;
		};
	}

	/**
	 * Tira o "/api" da frente antes de interpretar o caminho.
	 *
	 * A entidade e o id saem da posicao dos segmentos na URL. Quando a API
	 * ganhou o prefixo, o primeiro segmento passou a ser "api": sem esta
	 * linha, tudo seria gravado como entidade API e sem id -- e o titular,
	 * que consulta o proprio registro pela entidade PROFISSIONAIS, veria a
	 * lista vazia. Nada falharia; o registro so deixaria de servir. Quem
	 * vigia isso e o RegistroDeAcessoIT.
	 */
	private String semPrefixo(String caminho) {
		return caminho.startsWith(WebConfig.PREFIXO_API + "/")
				? caminho.substring(WebConfig.PREFIXO_API.length())
				: caminho;
	}

	/**
	 * O primeiro segmento do caminho identifica o recurso principal:
	 * /profissionais/3/contatos -> PROFISSIONAL. Guardar a entidade
	 * raiz mantem a consulta "tudo que aconteceu com o profissional 3"
	 * simples, sem precisar juntar varias tabelas de log.
	 */
	private String entidadeDe(String caminho) {
		String[] partes = caminho.split("/");
		return partes.length > 1 ? partes[1].toUpperCase() : "DESCONHECIDA";
	}

	/**
	 * Id do recurso principal, quando o caminho traz um:
	 * /profissionais/3/contatos/7 -> 3.
	 */
	private Long registroIdDe(String caminho) {
		String[] partes = caminho.split("/");
		if (partes.length > 2) {
			try {
				return Long.parseLong(partes[2]);
			} catch (NumberFormatException naoEhId) {
				return null;
			}
		}
		return null;
	}

}
