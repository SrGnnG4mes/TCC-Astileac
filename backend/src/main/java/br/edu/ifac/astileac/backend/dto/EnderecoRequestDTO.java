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
		@NotBlank(message = "Informe o CEP.")
		@Pattern(regexp = "\\d{8}", message = "O CEP deve ter 8 dígitos.")
		String cep,

		@NotBlank(message = "Informe o logradouro.")
		@Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres.")
		String logradouro,

		@NotBlank(message = "Informe o número.")
		@Size(max = 20, message = "O número deve ter no máximo 20 caracteres.")
		String numero,

		@Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres.")
		String complemento,

		@NotBlank(message = "Informe o bairro.")
		@Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.")
		String bairro,

		@NotBlank(message = "Informe a cidade.")
		@Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
		String cidade,

		@NotBlank(message = "Informe a UF.")
		@Pattern(regexp = "[A-Z]{2}", message = "A UF deve ter 2 letras maiúsculas, por exemplo: AC.")
		String uf
) {
}
