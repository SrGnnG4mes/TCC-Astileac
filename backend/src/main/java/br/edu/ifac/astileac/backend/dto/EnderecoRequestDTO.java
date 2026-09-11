package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada do endereco de um profissional (RF02).
 *
 * CEP so com digitos, sem hifen: a convencao do projeto e guardar o
 * dado limpo e formatar na apresentacao -- a mesma adotada para o CPF.
 */
public record EnderecoRequestDTO(
		@NotBlank(message = "cep e obrigatorio")
		@Pattern(regexp = "\\d{8}", message = "cep deve conter exatamente 8 digitos numericos, sem hifen")
		String cep,

		@NotBlank(message = "logradouro e obrigatorio")
		@Size(max = 150, message = "logradouro deve ter no maximo 150 caracteres")
		String logradouro,

		@NotBlank(message = "numero e obrigatorio")
		@Size(max = 20, message = "numero deve ter no maximo 20 caracteres")
		String numero,

		@Size(max = 100, message = "complemento deve ter no maximo 100 caracteres")
		String complemento,

		@NotBlank(message = "bairro e obrigatorio")
		@Size(max = 100, message = "bairro deve ter no maximo 100 caracteres")
		String bairro,

		@NotBlank(message = "cidade e obrigatoria")
		@Size(max = 100, message = "cidade deve ter no maximo 100 caracteres")
		String cidade,

		@NotBlank(message = "uf e obrigatoria")
		@Pattern(regexp = "[A-Z]{2}", message = "uf deve ter 2 letras maiusculas, ex.: AC")
		String uf
) {
}
