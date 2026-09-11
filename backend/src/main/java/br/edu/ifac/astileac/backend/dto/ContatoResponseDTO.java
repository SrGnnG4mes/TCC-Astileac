package br.edu.ifac.astileac.backend.dto;

import br.edu.ifac.astileac.backend.entity.TipoContato;

/**
 * Dados de saida de um contato.
 */
public record ContatoResponseDTO(
		Long id,
		TipoContato tipo,
		String valor,
		Boolean principal
) {
}
