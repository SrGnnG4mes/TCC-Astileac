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
		@NotBlank(message = "instituicao e obrigatoria")
		@Size(max = 150, message = "instituicao deve ter no maximo 150 caracteres")
		String instituicao,

		@NotBlank(message = "cargo e obrigatorio")
		@Size(max = 100, message = "cargo deve ter no maximo 100 caracteres")
		String cargo,

		@NotBlank(message = "tipoVinculo e obrigatorio")
		@Size(max = 50, message = "tipoVinculo deve ter no maximo 50 caracteres")
		String tipoVinculo,

		@NotNull(message = "dataInicio e obrigatoria")
		@PastOrPresent(message = "dataInicio nao pode estar no futuro")
		LocalDate dataInicio,

		/** Nula = vinculo em andamento. */
		@PastOrPresent(message = "dataFim nao pode estar no futuro")
		LocalDate dataFim
) {
}
