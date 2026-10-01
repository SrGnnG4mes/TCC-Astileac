package br.edu.ifac.astileac.backend.dto;

/**
 * Consulta publica do convite, antes de mostrar o formulario.
 *
 * Devolve o e-mail de destino para a pessoa conferir que o convite e
 * mesmo dela, e nada alem disso: um convite invalido nao revela se
 * chegou a existir, so que nao serve.
 */
public record ConviteValidacaoDTO(
		boolean valido,
		String email,
		String motivo
) {
}
