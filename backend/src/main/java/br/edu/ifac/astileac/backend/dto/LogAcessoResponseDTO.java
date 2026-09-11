package br.edu.ifac.astileac.backend.dto;

import br.edu.ifac.astileac.backend.entity.AcaoLog;
import java.time.LocalDateTime;

/**
 * Uma linha do registro de acesso (RF11).
 */
public record LogAcessoResponseDTO(
		Long id,
		LocalDateTime dataHora,
		String autor,
		String origem,
		AcaoLog acao,
		String entidade,
		Long registroId,
		String detalhe
) {
}
