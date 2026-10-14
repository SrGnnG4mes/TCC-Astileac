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
		@NotBlank(message = "Informe o nível da formação.")
		@Size(max = 50, message = "O nível deve ter no máximo 50 caracteres.")
		String nivel,

		@NotBlank(message = "Informe o curso.")
		@Size(max = 150, message = "O curso deve ter no máximo 150 caracteres.")
		String curso,

		@NotBlank(message = "Informe a instituição.")
		@Size(max = 150, message = "A instituição deve ter no máximo 150 caracteres.")
		String instituicao,

		@NotNull(message = "Informe o ano de conclusão.")
		Integer anoConclusao
) {
}
