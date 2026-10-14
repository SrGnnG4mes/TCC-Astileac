package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dados de entrada de um vinculo empregaticio (RF07).
 *
 * NAO ha campo "atual" aqui, de proposito. O CLAUDE.md define que
 * data_fim nula significa vinculo em andamento -- ou seja, "atual" ja
 * esta contido em data_fim. Aceitar os dois abriria espaco para o
 * cliente enviar data_fim preenchida com atual=true, e o banco
 * guardaria uma contradicao. O servico deriva "atual" de data_fim, e
 * a inconsistencia deixa de ser possivel.
 */
public record VinculoEmpregaticioRequestDTO(
		@NotBlank(message = "Informe a instituição.")
		@Size(max = 150, message = "A instituição deve ter no máximo 150 caracteres.")
		String instituicao,

		@NotBlank(message = "Informe o cargo.")
		@Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres.")
		String cargo,

		@NotBlank(message = "Informe o tipo de vínculo.")
		@Size(max = 50, message = "O tipo de vínculo deve ter no máximo 50 caracteres.")
		String tipoVinculo,

		@NotNull(message = "Informe a data de início.")
		@PastOrPresent(message = "A data de início não pode estar no futuro.")
		LocalDate dataInicio,

		/** Nula = vinculo em andamento. */
		@PastOrPresent(message = "A data de término não pode estar no futuro.")
		LocalDate dataFim
) {
}
