package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.ProfissionalRequestDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResponseDTO;
import br.edu.ifac.astileac.backend.service.ProfissionalService;
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
 * CRUD de profissional (RF01). Controller so traduz HTTP <-> DTO;
 * toda regra de negocio esta no ProfissionalService.
 */
@RestController
@RequestMapping("/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {

	private final ProfissionalService profissionalService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProfissionalResponseDTO criar(@Valid @RequestBody ProfissionalRequestDTO dto) {
		return profissionalService.criar(dto);
	}

	@GetMapping
	public List<ProfissionalResponseDTO> listar() {
		return profissionalService.listar();
	}

	@GetMapping("/{id}")
	public ProfissionalResponseDTO buscarPorId(@PathVariable Long id) {
		return profissionalService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public ProfissionalResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ProfissionalRequestDTO dto) {
		return profissionalService.atualizar(id, dto);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long id) {
		profissionalService.remover(id);
	}

}
