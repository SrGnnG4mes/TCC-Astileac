package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.ConviteEmitidoDTO;
import br.edu.ifac.astileac.backend.dto.ConviteRequestDTO;
import br.edu.ifac.astileac.backend.dto.ConviteResponseDTO;
import br.edu.ifac.astileac.backend.security.UsuarioLogado;
import br.edu.ifac.astileac.backend.service.ConviteService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Emissao e acompanhamento de convites (RF12).
 *
 * Tudo aqui e da diretoria. O lado de quem se associa esta no
 * CadastroController, que e publico por necessidade -- quem usa o
 * convite ainda nao tem conta.
 */
@RestController
@RequestMapping("/convites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ConviteController {

	private final ConviteService conviteService;
	private final UsuarioLogado usuarioLogado;

	/**
	 * Emite um convite para o e-mail informado.
	 *
	 * A resposta traz o token em texto, e e a unica vez que ele aparece:
	 * o banco guarda so o hash. Perdido o valor, o caminho e emitir
	 * outro convite.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ConviteEmitidoDTO emitir(@Valid @RequestBody ConviteRequestDTO dto) {
		return conviteService.emitir(dto.email(), usuarioLogado.loginAtual());
	}

	@GetMapping
	public List<ConviteResponseDTO> listar(
			@RequestParam(required = false, defaultValue = "false") boolean apenasPendentes) {
		return apenasPendentes ? conviteService.listarPendentes() : conviteService.listar();
	}
}
