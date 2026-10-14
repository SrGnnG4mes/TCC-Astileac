package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Emissao de convite (RF12): a diretoria informa so o e-mail de destino. */
public record ConviteRequestDTO(
		@NotBlank(message = "Informe o e-mail.")
		@Email(message = "E-mail inválido.")
		@Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
		String email
) {
}
