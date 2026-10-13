package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Situacao do profissional no quadro de associados.
 *
 * Um campo so, e deliberado: a operacao e idempotente e diz o estado
 * desejado, em vez de duas rotas ("desativar" e "reativar") que fariam
 * a mesma coisa em sentidos opostos.
 */
public record SituacaoRequestDTO(
		@NotNull(message = "ativo e obrigatorio")
		Boolean ativo
) {
}
