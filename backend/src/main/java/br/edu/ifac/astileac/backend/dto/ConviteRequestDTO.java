package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Emissao de convite (RF12): a diretoria informa so o e-mail de destino. */
public record ConviteRequestDTO(
		@NotBlank(message = "email e obrigatorio")
		@Email(message = "email invalido")
		@Size(max = 150, message = "email deve ter no maximo 150 caracteres")
		String email
) {
}
