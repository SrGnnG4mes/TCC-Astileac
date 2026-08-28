package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relacao 1:1 com profissional (profissional_id e UNIQUE na tabela).
 * Mapeamento unidirecional: Endereco conhece o Profissional, o
 * contrario nao (ver package-info do pacote entity).
 */
@Entity
@Table(name = "endereco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Endereco {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profissional_id", nullable = false, unique = true)
	private Profissional profissional;

	@Column(nullable = false, length = 8)
	private String cep;

	@Column(nullable = false, length = 150)
	private String logradouro;

	@Column(nullable = false, length = 20)
	private String numero;

	@Column(length = 100)
	private String complemento;

	@Column(nullable = false, length = 100)
	private String bairro;

	@Column(nullable = false, length = 100)
	private String cidade;

	// @JdbcTypeCode explicito: a coluna e CHAR(2) no banco (ver
	// V1__criar_schema_inicial.sql), nao VARCHAR. columnDefinition sozinho
	// nao bastaria: ele so afeta a geracao de DDL, a validacao do
	// ddl-auto=validate compara o tipo JDBC mapeado, que por padrao e
	// VARCHAR pra qualquer campo String.
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 2)
	private String uf;

}
