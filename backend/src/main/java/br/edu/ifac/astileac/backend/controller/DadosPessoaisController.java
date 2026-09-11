package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.DadosPessoaisDTO;
import br.edu.ifac.astileac.backend.dto.ExclusaoRespostaDTO;
import br.edu.ifac.astileac.backend.dto.LogAcessoResponseDTO;
import br.edu.ifac.astileac.backend.service.DadosPessoaisService;
import br.edu.ifac.astileac.backend.service.LogAcessoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direitos do titular dos dados (RF10, LGPD Art. 18) e consulta ao
 * registro de acesso (RF11, Art. 46).
 *
 * Agrupado num controller proprio, e nao espalhado no
 * ProfissionalController, porque nao e cadastro: e o atendimento a um
 * direito. Quando a autenticacao existir (Passo 6), estas rotas terao
 * regra de acesso propria -- o titular alcanca so os proprios dados.
 */
@RestController
@RequestMapping("/profissionais/{profissionalId}/dados-pessoais")
@RequiredArgsConstructor
public class DadosPessoaisController {

	private final DadosPessoaisService dadosPessoaisService;
	private final LogAcessoService logAcessoService;

	/** Art. 18, II e V: acesso e portabilidade. */
	@GetMapping
	public DadosPessoaisDTO exportar(@PathVariable Long profissionalId) {
		return dadosPessoaisService.exportar(profissionalId);
	}

	/**
	 * Art. 18, VI: eliminacao. Devolve 200 com a explicacao do prazo, e
	 * nao 204 sem corpo: a pessoa precisa saber que os dados ficam 30
	 * dias guardados e como desfazer o pedido.
	 */
	@DeleteMapping("/exclusao")
	public ExclusaoRespostaDTO solicitarExclusao(@PathVariable Long profissionalId) {
		return dadosPessoaisService.solicitarExclusao(profissionalId);
	}

	@PostMapping("/exclusao/cancelamento")
	public ExclusaoRespostaDTO cancelarExclusao(@PathVariable Long profissionalId) {
		return dadosPessoaisService.cancelarExclusao(profissionalId);
	}

	/** Art. 18, I: confirmacao de que ha tratamento, e qual. */
	@GetMapping("/registro-de-acesso")
	public Page<LogAcessoResponseDTO> registroDeAcesso(
			@PathVariable Long profissionalId,
			@PageableDefault(size = 50, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
		return logAcessoService.consultarPorRegistro(LogAcessoService.ENTIDADE_PROFISSIONAL, profissionalId, pageable);
	}

}
