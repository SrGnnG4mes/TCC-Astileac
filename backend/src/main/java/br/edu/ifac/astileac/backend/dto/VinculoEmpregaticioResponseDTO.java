package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;

/**
 * Vinculo empregaticio (RF07). dataFim nula = vinculo em andamento; o
 * historico e mantido, nao so o vinculo atual.
 */
public record VinculoEmpregaticioResponseDTO(
		Long id,
		String instituicao,
		String cargo,
		String tipoVinculo,
		LocalDate dataInicio,
		LocalDate dataFim,
		Boolean atual
) {
}
