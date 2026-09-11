package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.ProfissionalRequestDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResponseDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResumoDTO;
import br.edu.ifac.astileac.backend.service.ProfissionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	/**
	 * Consulta com filtros opcionais (RF08):
	 * /profissionais?area=juridica&amp;nome=maria&amp;ativo=true&amp;page=0&amp;size=20
	 *
	 * Sem nenhum parametro, e a listagem geral. Devolve o resumo com
	 * CPF mascarado -- a ficha completa e o buscarPorId.
	 */
	@GetMapping
	public Page<ProfissionalResumoDTO> consultar(
			@RequestParam(required = false) String area,
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Boolean ativo,
			@PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return profissionalService.consultar(nome, ativo, area, pageable);
	}

	@PreAuthorize("hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#id)")
	@GetMapping("/{id}")
	public ProfissionalResponseDTO buscarPorId(@PathVariable Long id) {
		return profissionalService.buscarPorId(id);
	}

	@PreAuthorize("hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#id)")
	@PutMapping("/{id}")
	public ProfissionalResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ProfissionalRequestDTO dto) {
		return profissionalService.atualizar(id, dto);
	}

	// Exclusao administrativa. O associado que quer sair usa o pedido
	// de eliminacao do RF10, que tem janela de arrependimento.
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long id) {
		profissionalService.remover(id);
	}

}
