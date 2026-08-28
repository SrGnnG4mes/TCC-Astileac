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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relacao 1:1 OPCIONAL com profissional (profissional_id aceita nulo
 * no schema -- nem todo usuario do sistema precisa ser um profissional
 * cadastrado, ex.: contas administrativas).
 *
 * senha_hash guarda sempre um hash BCrypt (via Spring Security),
 * nunca a senha em texto puro.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profissional_id", unique = true)
	private Profissional profissional;

	@Column(nullable = false, unique = true, length = 100)
	private String login;

	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	@Column(nullable = false, length = 30)
	private String perfil;

}
