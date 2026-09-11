package br.edu.ifac.astileac.backend.dto;

/**
 * Formacao concluida (RF05). Junto com os certificados, e o que
 * comprova a habilitacao exigida pela Lei 14.704/2023.
 */
public record FormacaoResponseDTO(
		Long id,
		String nivel,
		String curso,
		String instituicao,
		Integer anoConclusao
) {
}
