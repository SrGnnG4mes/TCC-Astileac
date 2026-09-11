package br.edu.ifac.astileac.backend.exception;

/**
 * Lancada quando os dados chegam bem formados, mas violam uma regra do
 * dominio -- por exemplo um contato do tipo EMAIL cujo valor nao e um
 * e-mail, ou um vinculo cuja data de fim vem antes da de inicio.
 *
 * Diferente do Bean Validation, que olha cada campo isoladamente: aqui
 * a regra depende de outros campos ou do estado do banco. O
 * GlobalExceptionHandler traduz para HTTP 400.
 */
public class RegraDeNegocioException extends RuntimeException {

	public RegraDeNegocioException(String mensagem) {
		super(mensagem);
	}

}
