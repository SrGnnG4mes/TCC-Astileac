package br.edu.ifac.astileac.backend.dto;

/**
 * Corpo padrao de resposta de erro da API, usado pelo GlobalExceptionHandler.
 */
public record ErroRespostaDTO(
		int status,
		String mensagem
) {
}
