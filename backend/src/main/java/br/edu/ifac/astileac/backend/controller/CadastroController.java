package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.CadastroPorConviteDTO;
import br.edu.ifac.astileac.backend.dto.ConviteValidacaoDTO;
import br.edu.ifac.astileac.backend.service.ConviteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro feito pelo proprio associado, com o convite em maos (RF12).
 *
 * E publico por necessidade: quem usa o convite ainda nao tem conta. O
 * que faz as vezes de autenticacao aqui e o proprio token -- emitido
 * pela diretoria depois do pagamento, de uso unico e com prazo.
 *
 * As DUAS operacoes sao POST, inclusive a validacao, que logicamente
 * seria um GET. O motivo e nao colocar o token na URL: caminho e query
 * string aparecem no log de acesso do servidor, no historico de proxy e
 * no Referer. O token e credencial portadora -- quem o le, usa.
 */
@RestController
@RequestMapping("/cadastro")
@RequiredArgsConstructor
public class CadastroController {

	private final ConviteService conviteService;

	/**
	 * Diz se vale mostrar o formulario, e para qual e-mail o convite
	 * foi emitido -- para a pessoa conferir que e mesmo dela.
	 *
	 * Convite invalido nao revela se chegou a existir: a resposta e a
	 * mesma para token inventado e para token ja usado.
	 */
	@PostMapping("/validacao")
	public ConviteValidacaoDTO validar(@Valid @RequestBody TokenDTO dto) {
		return conviteService.validar(dto.token());
	}

	/**
	 * Consome o convite e cria cadastro e conta de uma vez.
	 *
	 * Devolve so o id e uma orientacao: nenhum dado pessoal volta aqui,
	 * porque quem chamou ainda nao esta autenticado -- a sessao so
	 * comeca no login seguinte.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Object> cadastrar(@Valid @RequestBody CadastroPorConviteDTO dto) {
		Long id = conviteService.cadastrar(dto);
		return Map.of(
				"profissionalId", id,
				"mensagem", "Cadastro criado. Agora entre no sistema com o login e a senha que voce escolheu.");
	}

	public record TokenDTO(@NotBlank(message = "token e obrigatorio") String token) {
	}
}
