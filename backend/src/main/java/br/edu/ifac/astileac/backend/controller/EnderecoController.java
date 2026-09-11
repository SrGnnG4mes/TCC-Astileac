package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.EnderecoRequestDTO;
import br.edu.ifac.astileac.backend.dto.EnderecoResponseDTO;
import br.edu.ifac.astileac.backend.service.EnderecoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endereco do profissional (RF02).
 *
 * Rota aninhada e no SINGULAR: um endereco nao existe solto, e so
 * cabe um por profissional. A propria URL comunica a cardinalidade,
 * por isso tambem nao existe POST aqui -- ver o PUT abaixo.
 */
// O associado alcanca so os proprios dados; a diretoria alcanca
// qualquer um. Como todo metodo desta classe recebe profissionalId,
// uma anotacao de classe cobre todos eles (RF09).
@PreAuthorize("hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#profissionalId)")
@RestController
@RequestMapping("/profissionais/{profissionalId}/endereco")
@RequiredArgsConstructor
public class EnderecoController {

	private final EnderecoService enderecoService;

	/**
	 * Cria ou substitui. PUT em vez de POST porque o recurso e unico e
	 * a operacao e idempotente: o cliente sabe o endereco final do
	 * recurso antes de chamar.
	 */
	@PutMapping
	public EnderecoResponseDTO salvar(
			@PathVariable Long profissionalId, @Valid @RequestBody EnderecoRequestDTO dto) {
		return enderecoService.salvar(profissionalId, dto);
	}

	@GetMapping
	public EnderecoResponseDTO buscar(@PathVariable Long profissionalId) {
		return enderecoService.buscar(profissionalId);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long profissionalId) {
		enderecoService.remover(profissionalId);
	}

}
