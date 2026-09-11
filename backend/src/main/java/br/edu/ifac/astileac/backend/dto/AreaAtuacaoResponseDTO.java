package br.edu.ifac.astileac.backend.dto;

/**
 * Dados de saida de uma area de atuacao.
 */
public record AreaAtuacaoResponseDTO(
		Long id,
		String nome,
		String descricao
) {
}
