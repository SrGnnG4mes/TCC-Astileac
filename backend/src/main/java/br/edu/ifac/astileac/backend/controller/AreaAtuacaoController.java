package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.AreaAtuacaoRequestDTO;
import br.edu.ifac.astileac.backend.dto.AreaAtuacaoResponseDTO;
import br.edu.ifac.astileac.backend.service.AreaAtuacaoService;
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
 * Areas de atuacao (tabela de dominio).
 *
 * Rota de primeiro nivel, nao aninhada: uma area existe por si so,
 * independente de haver profissionais nela -- diferente de endereco e
 * contatos, que so fazem sentido dentro de um profissional.
 */
@RestController
@RequestMapping("/areas-atuacao")
@RequiredArgsConstructor
public class AreaAtuacaoController {

	private final AreaAtuacaoService areaAtuacaoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AreaAtuacaoResponseDTO criar(@Valid @RequestBody AreaAtuacaoRequestDTO dto) {
		return areaAtuacaoService.criar(dto);
	}

	@GetMapping
	public List<AreaAtuacaoResponseDTO> listar() {
		return areaAtuacaoService.listar();
	}

	@GetMapping("/{id}")
	public AreaAtuacaoResponseDTO buscarPorId(@PathVariable Long id) {
		return areaAtuacaoService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public AreaAtuacaoResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody AreaAtuacaoRequestDTO dto) {
		return areaAtuacaoService.atualizar(id, dto);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long id) {
		areaAtuacaoService.remover(id);
	}

}
