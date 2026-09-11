package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;

/**
 * Dados de saida de um profissional.
 */
public record ProfissionalResponseDTO(
		Long id,
		String nome,
		String cpf,
		LocalDate dataNascimento,
		LocalDate dataAssociacao,
		Boolean ativo
) {
}
