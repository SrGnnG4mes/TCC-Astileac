package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dados de saida de um profissional (ficha completa).
 *
 * dataExclusaoSolicitada preenchida significa que o titular pediu a
 * eliminacao dos dados e o cadastro esta na janela de arrependimento:
 * bloqueado para consultas, mas ainda recuperavel. Sem esse campo, a
 * associacao veria apenas ativo=false e nao saberia o motivo.
 */
public record ProfissionalResponseDTO(
		Long id,
		String nome,
		String cpf,
		LocalDate dataNascimento,
		LocalDate dataAssociacao,
		Boolean ativo,
		LocalDateTime dataExclusaoSolicitada,

		/** Endereco do curriculo Lattes. Nulo quando o associado nao informou. */
		String lattesUrl
) {
}
