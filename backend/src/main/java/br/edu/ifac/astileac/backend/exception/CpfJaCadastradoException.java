package br.edu.ifac.astileac.backend.exception;

/**
 * Lancada ao tentar cadastrar/atualizar um profissional com um CPF
 * que ja pertence a outro registro (cpf e UNIQUE na tabela profissional).
 * O GlobalExceptionHandler traduz isso para HTTP 409 (Conflict).
 */
public class CpfJaCadastradoException extends RuntimeException {

	public CpfJaCadastradoException(String cpf) {
		super("Ja existe um profissional cadastrado com o CPF " + cpf);
	}

}
