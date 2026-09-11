package br.edu.ifac.astileac.backend.exception;

/**
 * Lancada quando um recurso buscado por id (profissional, formacao,
 * etc.) nao existe. O GlobalExceptionHandler traduz isso para HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
