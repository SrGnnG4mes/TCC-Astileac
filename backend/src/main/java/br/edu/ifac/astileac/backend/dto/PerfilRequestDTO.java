package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Concessao ou retirada do perfil administrativo (RF12).
 *
 * A diretoria descreveu o perfil como "status da conta, que dependendo
 * libera ou tira recursos" -- por isso e uma alteracao de atributo da
 * conta, e nao a criacao de outro tipo de usuario.
 */
public record PerfilRequestDTO(
		@NotBlank(message = "Informe o perfil.")
		String perfil
) {
}
