package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade central do modelo. Mapeia a tabela profissional criada em
 * V1__criar_schema_inicial.sql.
 */
@Entity
@Table(name = "profissional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Profissional {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(nullable = false, unique = true, length = 11)
	private String cpf;

	@Column(name = "data_nascimento", nullable = false)
	private LocalDate dataNascimento;

	@Column(name = "data_associacao", nullable = false)
	private LocalDate dataAssociacao;

	@Column(nullable = false)
	private Boolean ativo = true;

	/**
	 * Quando o titular pediu a eliminacao dos seus dados (LGPD Art. 18,
	 * VI). Nulo = cadastro normal. Preenchido = janela de
	 * arrependimento: os dados ficam bloqueados por 30 dias e, passado
	 * o prazo, sao apagados definitivamente.
	 */
	@Column(name = "data_exclusao_solicitada")
	private LocalDateTime dataExclusaoSolicitada;

}
