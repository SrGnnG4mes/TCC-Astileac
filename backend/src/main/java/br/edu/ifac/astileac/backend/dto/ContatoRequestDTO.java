package br.edu.ifac.astileac.backend.dto;

import br.edu.ifac.astileac.backend.entity.TipoContato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada de um contato (RF03).
 *
 * O formato de "valor" depende de "tipo": e-mail precisa parecer
 * e-mail, telefone precisa ser digito. Bean Validation olha um campo
 * de cada vez, entao essa checagem cruzada fica no ContatoService,
 * como regra de negocio.
 */
public record ContatoRequestDTO(
		@NotNull(message = "Informe o tipo de contato: e-mail ou telefone.")
		TipoContato tipo,

		@NotBlank(message = "Informe o contato.")
		@Size(max = 150, message = "O contato deve ter no máximo 150 caracteres.")
		String valor,

		/** Nulo equivale a false: contato comum, nao o principal. */
		Boolean principal
) {
}
