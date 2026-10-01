package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca da propria senha (RF12).
 *
 * Exige a senha atual de proposito: sem isso, uma sessao sequestrada
 * trocaria a senha e trancaria o dono para fora da propria conta.
 */
public record SenhaRequestDTO(
		@NotBlank(message = "senha atual e obrigatoria")
		String senhaAtual,

		@NotBlank(message = "senha nova e obrigatoria")
		@Size(min = 8, max = 100, message = "senha nova deve ter no minimo 8 caracteres")
		String senhaNova
) {
}
