package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada de uma area de atuacao (tabela de dominio).
 *
 * A carga inicial (V2) traz educacional, juridica, saude,
 * artistico-cultural e outras, mas a associacao pode cadastrar novas
 * sem alterar o schema -- e justamente por isso que a relacao com
 * profissional e N:N, e nao uma coluna booleana por area.
 */
public record AreaAtuacaoRequestDTO(
		@NotBlank(message = "Informe o nome.")
		@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres.")
		String nome,

		@Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
		String descricao
) {
}
