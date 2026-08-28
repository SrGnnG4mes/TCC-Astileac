package br.edu.ifac.astileac.backend.entity;

/**
 * Os dois valores aceitos pela coluna contato.tipo (ver o CHECK
 * constraint em V1__criar_schema_inicial.sql).
 */
public enum TipoContato {
	EMAIL,
	TELEFONE
}
