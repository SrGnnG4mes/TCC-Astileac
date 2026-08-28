package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relacao 1:N com profissional. Registra formacoes ja concluidas,
 * base para a comprovacao de habilitacao exigida pela Lei 14.704/2023.
 */
@Entity
@Table(name = "formacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Formacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@Column(nullable = false, length = 50)
	private String nivel;

	@Column(nullable = false, length = 150)
	private String curso;

	@Column(nullable = false, length = 150)
	private String instituicao;

	@Column(name = "ano_conclusao", nullable = false)
	private Integer anoConclusao;

}
