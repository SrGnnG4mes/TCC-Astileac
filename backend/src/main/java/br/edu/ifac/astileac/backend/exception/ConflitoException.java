package br.edu.ifac.astileac.backend.exception;

/**
 * Lancada quando a operacao conflita com o estado atual: um registro
 * unico que ja existe, uma associacao repetida. Traduzida para HTTP
 * 409 (Conflict).
 *
 * Generica de proposito -- cada cadastro do Passo 4 tem a sua versao
 * de "isso ja esta cadastrado", e nao compensa uma classe de excecao
 * para cada uma.
 */
public class ConflitoException extends RuntimeException {

	public ConflitoException(String mensagem) {
		super(mensagem);
	}

}
