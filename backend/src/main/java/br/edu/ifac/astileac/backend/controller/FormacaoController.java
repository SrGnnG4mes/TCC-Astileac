package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.FormacaoRequestDTO;
import br.edu.ifac.astileac.backend.dto.FormacaoResponseDTO;
import br.edu.ifac.astileac.backend.service.FormacaoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Formacoes concluidas do profissional (RF05).
 *
 * Rota aninhada no plural: relacao 1:N, e o registro nao existe fora
 * de um profissional. Listas curtas, entao sem paginacao.
 */
// O associado alcanca so os proprios dados; a diretoria alcanca
// qualquer um. Como todo metodo desta classe recebe profissionalId,
// uma anotacao de classe cobre todos eles (RF09).
@PreAuthorize("hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#profissionalId)")
@RestController
@RequestMapping("/profissionais/{profissionalId}/formacoes")
@RequiredArgsConstructor
public class FormacaoController {

	private final FormacaoService formacaoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FormacaoResponseDTO criar(
			@PathVariable Long profissionalId, @Valid @RequestBody FormacaoRequestDTO dto) {
		return formacaoService.criar(profissionalId, dto);
	}

	@GetMapping
	public List<FormacaoResponseDTO> listar(@PathVariable Long profissionalId) {
		return formacaoService.listar(profissionalId);
	}

	@PutMapping("/{formacaoId}")
	public FormacaoResponseDTO atualizar(
			@PathVariable Long profissionalId,
			@PathVariable Long formacaoId,
			@Valid @RequestBody FormacaoRequestDTO dto) {
		return formacaoService.atualizar(profissionalId, formacaoId, dto);
	}

	@DeleteMapping("/{formacaoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long profissionalId, @PathVariable Long formacaoId) {
		formacaoService.remover(profissionalId, formacaoId);
	}

}
