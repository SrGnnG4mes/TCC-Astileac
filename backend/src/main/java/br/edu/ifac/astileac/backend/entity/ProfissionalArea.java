package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ponto central do modelo (ver CLAUDE.md secao 4): relacao N:N entre
 * profissional e area_atuacao, com data_inicio como atributo da
 * propria associacao. Chave composta via @EmbeddedId; @MapsId liga
 * cada @ManyToOne a parte correspondente da chave.
 */
@Entity
@Table(name = "profissional_area")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalArea {

	@EmbeddedId
	private ProfissionalAreaId id = new ProfissionalAreaId();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("profissionalId")
	@JoinColumn(name = "profissional_id")
	private Profissional profissional;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("areaAtuacaoId")
	@JoinColumn(name = "area_atuacao_id")
	private AreaAtuacao areaAtuacao;

	@Column(name = "data_inicio", nullable = false)
	private LocalDate dataInicio;

}
