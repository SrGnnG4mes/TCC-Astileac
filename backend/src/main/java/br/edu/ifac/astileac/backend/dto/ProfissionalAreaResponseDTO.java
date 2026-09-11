package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;

/**
 * Uma area em que o profissional atua, com desde quando.
 *
 * Traz o nome da area junto do id para a tela nao precisar de uma
 * segunda consulta so para descobrir como se chama cada uma.
 */
public record ProfissionalAreaResponseDTO(
		Long areaAtuacaoId,
		String nome,
		LocalDate dataInicio
) {
}
