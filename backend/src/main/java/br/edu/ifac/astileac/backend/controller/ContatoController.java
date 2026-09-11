package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.ContatoRequestDTO;
import br.edu.ifac.astileac.backend.dto.ContatoResponseDTO;
import br.edu.ifac.astileac.backend.service.ContatoService;
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
 * Contatos do profissional (RF03).
 *
 * Rota aninhada no PLURAL, ao contrario de /endereco: aqui cabem
 * varios. A lista costuma ser curta (alguns e-mails e telefones),
 * entao nao ha paginacao.
 */
// O associado alcanca so os proprios dados; a diretoria alcanca
// qualquer um. Como todo metodo desta classe recebe profissionalId,
// uma anotacao de classe cobre todos eles (RF09).
@PreAuthorize("hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#profissionalId)")
@RestController
@RequestMapping("/profissionais/{profissionalId}/contatos")
@RequiredArgsConstructor
public class ContatoController {

	private final ContatoService contatoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ContatoResponseDTO criar(
			@PathVariable Long profissionalId, @Valid @RequestBody ContatoRequestDTO dto) {
		return contatoService.criar(profissionalId, dto);
	}

	@GetMapping
	public List<ContatoResponseDTO> listar(@PathVariable Long profissionalId) {
		return contatoService.listar(profissionalId);
	}

	@PutMapping("/{contatoId}")
	public ContatoResponseDTO atualizar(
			@PathVariable Long profissionalId,
			@PathVariable Long contatoId,
			@Valid @RequestBody ContatoRequestDTO dto) {
		return contatoService.atualizar(profissionalId, contatoId, dto);
	}

	@DeleteMapping("/{contatoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long profissionalId, @PathVariable Long contatoId) {
		contatoService.remover(profissionalId, contatoId);
	}

}
