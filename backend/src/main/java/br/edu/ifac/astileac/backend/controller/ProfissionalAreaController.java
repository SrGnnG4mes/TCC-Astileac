package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.ProfissionalAreaRequestDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalAreaResponseDTO;
import br.edu.ifac.astileac.backend.service.ProfissionalAreaService;
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
 * Areas em que um profissional atua (RF04).
 *
 * O identificador do vinculo na URL e o id da AREA, nao um id proprio:
 * a chave do registro e o par (profissional, area), entao
 * /profissionais/3/areas/2 ja identifica exatamente uma linha.
 */
@RestController
@RequestMapping("/profissionais/{profissionalId}/areas")
@RequiredArgsConstructor
public class ProfissionalAreaController {

	private final ProfissionalAreaService profissionalAreaService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProfissionalAreaResponseDTO associar(
			@PathVariable Long profissionalId, @Valid @RequestBody ProfissionalAreaRequestDTO dto) {
		return profissionalAreaService.associar(profissionalId, dto);
	}

	@GetMapping
	public List<ProfissionalAreaResponseDTO> listar(@PathVariable Long profissionalId) {
		return profissionalAreaService.listar(profissionalId);
	}

	@PutMapping("/{areaAtuacaoId}")
	public ProfissionalAreaResponseDTO atualizarDataInicio(
			@PathVariable Long profissionalId,
			@PathVariable Long areaAtuacaoId,
			@Valid @RequestBody ProfissionalAreaRequestDTO dto) {
		return profissionalAreaService.atualizarDataInicio(profissionalId, areaAtuacaoId, dto);
	}

	@DeleteMapping("/{areaAtuacaoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desassociar(@PathVariable Long profissionalId, @PathVariable Long areaAtuacaoId) {
		profissionalAreaService.desassociar(profissionalId, areaAtuacaoId);
	}

}
