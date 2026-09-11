package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Dados para vincular um profissional a uma area de atuacao (RF04).
 *
 * data_inicio e atributo da associacao, nao do profissional nem da
 * area: registra desde quando aquela pessoa atua naquela area
 * especifica. E o motivo de PROFISSIONAL_AREA ser uma tabela com
 * chave composta, e nao uma simples lista de ids.
 */
public record ProfissionalAreaRequestDTO(
		@NotNull(message = "areaAtuacaoId e obrigatorio")
		Long areaAtuacaoId,

		@NotNull(message = "dataInicio e obrigatoria")
		@PastOrPresent(message = "dataInicio nao pode estar no futuro")
		LocalDate dataInicio
) {
}
