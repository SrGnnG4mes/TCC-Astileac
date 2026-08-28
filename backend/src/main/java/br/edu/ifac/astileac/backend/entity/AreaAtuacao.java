package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabela de dominio. Carga inicial em V2__carga_inicial_areas_atuacao.sql
 * (educacional, juridica, saude, artistico-cultural, outras), mas novas
 * areas podem ser cadastradas sem alterar o schema.
 */
@Entity
@Table(name = "area_atuacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AreaAtuacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String nome;

	@Column(length = 255)
	private String descricao;

}
