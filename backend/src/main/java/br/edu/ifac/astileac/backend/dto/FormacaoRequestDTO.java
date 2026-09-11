package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada de uma formacao concluida (RF05).
 *
 * Junto com os certificados, e o que comprova a habilitacao exigida
 * pela Lei 14.704/2023 -- nao e um campo secundario do cadastro.
 *
 * "nivel" e texto livre porque o banco tambem nao restringe os valores.
 * Transformar numa lista controlada (graduacao, pos-graduacao,
 * mestrado...) exigiria migration com CHECK, e e decisao da associacao,
 * nao do codigo.
 */
public record FormacaoRequestDTO(
		@NotBlank(message = "nivel e obrigatorio")
		@Size(max = 50, message = "nivel deve ter no maximo 50 caracteres")
		String nivel,

		@NotBlank(message = "curso e obrigatorio")
		@Size(max = 150, message = "curso deve ter no maximo 150 caracteres")
		String curso,

		@NotBlank(message = "instituicao e obrigatoria")
		@Size(max = 150, message = "instituicao deve ter no maximo 150 caracteres")
		String instituicao,

		@NotNull(message = "anoConclusao e obrigatorio")
		Integer anoConclusao
) {
}
