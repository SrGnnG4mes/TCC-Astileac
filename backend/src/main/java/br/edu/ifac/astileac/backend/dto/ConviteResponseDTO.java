package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDateTime;

/**
 * Convite na listagem da diretoria. Sem token: ele existe em texto
 * apenas no instante da emissao.
 */
public record ConviteResponseDTO(
		Long id,
		String email,
		LocalDateTime dataEmissao,
		LocalDateTime dataExpiracao,
		LocalDateTime dataConsumo,
		String situacao,
		Long profissionalId
) {
}
