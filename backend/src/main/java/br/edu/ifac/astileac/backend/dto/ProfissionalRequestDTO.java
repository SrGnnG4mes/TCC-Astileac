package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dados de entrada para criar/atualizar um profissional (RF01).
 * "ativo" nao entra aqui de proposito: todo profissional novo comeca
 * ativo, e a desativacao e uma acao propria, nao um campo livre no
 * formulario de cadastro.
 */
public record ProfissionalRequestDTO(
		@NotBlank(message = "nome e obrigatorio")
		@Size(max = 150, message = "nome deve ter no maximo 150 caracteres")
		String nome,

		@NotBlank(message = "cpf e obrigatorio")
		@Pattern(regexp = "\\d{11}", message = "cpf deve conter exatamente 11 digitos numericos")
		String cpf,

		@NotNull(message = "data de nascimento e obrigatoria")
		@Past(message = "data de nascimento deve estar no passado")
		LocalDate dataNascimento,

		@NotNull(message = "data de associacao e obrigatoria")
		LocalDate dataAssociacao
) {
}
