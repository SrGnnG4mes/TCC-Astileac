package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.VinculoEmpregaticioRequestDTO;
import br.edu.ifac.astileac.backend.dto.VinculoEmpregaticioResponseDTO;
import br.edu.ifac.astileac.backend.service.VinculoEmpregaticioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * Vinculos empregaticios do profissional (RF07).
 *
 * Rota aninhada no plural: relacao 1:N, e o registro nao existe fora
 * de um profissional. Listas curtas, entao sem paginacao.
 */
@RestController
@RequestMapping("/profissionais/{profissionalId}/vinculos")
@RequiredArgsConstructor
public class VinculoEmpregaticioController {

	private final VinculoEmpregaticioService vinculoEmpregaticioService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VinculoEmpregaticioResponseDTO criar(
			@PathVariable Long profissionalId, @Valid @RequestBody VinculoEmpregaticioRequestDTO dto) {
		return vinculoEmpregaticioService.criar(profissionalId, dto);
	}

	@GetMapping
	public List<VinculoEmpregaticioResponseDTO> listar(@PathVariable Long profissionalId) {
		return vinculoEmpregaticioService.listar(profissionalId);
	}

	@PutMapping("/{vinculoId}")
	public VinculoEmpregaticioResponseDTO atualizar(
			@PathVariable Long profissionalId,
			@PathVariable Long vinculoId,
			@Valid @RequestBody VinculoEmpregaticioRequestDTO dto) {
		return vinculoEmpregaticioService.atualizar(profissionalId, vinculoId, dto);
	}

	@DeleteMapping("/{vinculoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long profissionalId, @PathVariable Long vinculoId) {
		vinculoEmpregaticioService.remover(profissionalId, vinculoId);
	}

}
