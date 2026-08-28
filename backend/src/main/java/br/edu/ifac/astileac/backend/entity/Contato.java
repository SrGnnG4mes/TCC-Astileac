package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Relacao 1:N com profissional. Um profissional pode ter varios
 * contatos (varios e-mails e/ou telefones).
 */
@Entity
@Table(name = "contato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contato {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoContato tipo;

	@Column(nullable = false, length = 150)
	private String valor;

	@Column(nullable = false)
	private Boolean principal = false;

}
