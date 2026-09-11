package br.edu.ifac.astileac.backend.entity;

/**
 * O que foi feito com o dado pessoal. Derivado do metodo HTTP da
 * requisicao, exceto EXPORTACAO, que e marcada explicitamente por ser
 * o atendimento a um direito do titular (LGPD Art. 18, V).
 */
public enum AcaoLog {
	CONSULTA,
	CRIACAO,
	ALTERACAO,
	EXCLUSAO,
	EXPORTACAO
}
