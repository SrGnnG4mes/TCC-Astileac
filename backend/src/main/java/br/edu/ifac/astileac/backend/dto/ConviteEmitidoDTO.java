package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDateTime;

/**
 * Resposta da emissao -- a UNICA vez que o token aparece em texto.
 *
 * O banco guarda so o hash, entao nao ha como recuperar este valor
 * depois. Se a diretoria perder o link, o caminho e emitir outro
 * convite, nao consultar este.
 */
public record ConviteEmitidoDTO(
		Long id,
		String email,
		String token,
		LocalDateTime dataExpiracao,
		String comoEntregar
) {
}
