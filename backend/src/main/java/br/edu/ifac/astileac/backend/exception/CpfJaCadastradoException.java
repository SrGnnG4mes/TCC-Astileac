package br.edu.ifac.astileac.backend.exception;

/**
 * Lancada ao tentar cadastrar/atualizar um profissional com um CPF que
 * ja pertence a outro registro (cpf e UNIQUE na tabela profissional).
 * O GlobalExceptionHandler traduz isso para HTTP 409 (Conflict).
 *
 * A mensagem NAO repete o CPF recebido, de proposito. Repetir o valor
 * transformaria a diferenca entre 201 e 409 em uma consulta: bastaria
 * testar CPFs para descobrir quem e associado da ASTILEAC, o que expoe
 * filiacao a entidade profissional, nao so o numero. Quem chamou a API
 * ja sabe qual CPF enviou.
 */
public class CpfJaCadastradoException extends ConflitoException {

	public CpfJaCadastradoException() {
		super("Ja existe um profissional cadastrado com este CPF");
	}

}
