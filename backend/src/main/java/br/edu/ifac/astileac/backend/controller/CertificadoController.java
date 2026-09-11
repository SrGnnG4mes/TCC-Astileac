package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.CertificadoRequestDTO;
import br.edu.ifac.astileac.backend.dto.CertificadoResponseDTO;
import br.edu.ifac.astileac.backend.service.CertificadoService;
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
 * Certificados do profissional (RF06).
 *
 * Rota aninhada no plural: relacao 1:N, e o registro nao existe fora
 * de um profissional. Listas curtas, entao sem paginacao.
 */
@RestController
@RequestMapping("/profissionais/{profissionalId}/certificados")
@RequiredArgsConstructor
public class CertificadoController {

	private final CertificadoService certificadoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CertificadoResponseDTO criar(
			@PathVariable Long profissionalId, @Valid @RequestBody CertificadoRequestDTO dto) {
		return certificadoService.criar(profissionalId, dto);
	}

	@GetMapping
	public List<CertificadoResponseDTO> listar(@PathVariable Long profissionalId) {
		return certificadoService.listar(profissionalId);
	}

	@PutMapping("/{certificadoId}")
	public CertificadoResponseDTO atualizar(
			@PathVariable Long profissionalId,
			@PathVariable Long certificadoId,
			@Valid @RequestBody CertificadoRequestDTO dto) {
		return certificadoService.atualizar(profissionalId, certificadoId, dto);
	}

	@DeleteMapping("/{certificadoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long profissionalId, @PathVariable Long certificadoId) {
		certificadoService.remover(profissionalId, certificadoId);
	}

}
