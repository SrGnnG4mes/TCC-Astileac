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
		@NotBlank(message = "nome e obrigatorio")
		@Size(max = 50, message = "nome deve ter no maximo 50 caracteres")
		String nome,

		@Size(max = 255, message = "descricao deve ter no maximo 255 caracteres")
		String descricao
) {
}
