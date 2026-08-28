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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relacao 1:N com profissional. data_fim nula = vinculo em andamento;
 * o historico completo e mantido, nao so o vinculo atual.
 */
@Entity
@Table(name = "vinculo_empregaticio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VinculoEmpregaticio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@Column(nullable = false, length = 150)
	private String instituicao;

	@Column(nullable = false, length = 100)
	private String cargo;

	@Column(name = "tipo_vinculo", nullable = false, length = 50)
	private String tipoVinculo;

	@Column(name = "data_inicio", nullable = false)
	private LocalDate dataInicio;

	@Column(name = "data_fim")
	private LocalDate dataFim;

	@Column(nullable = false)
	private Boolean atual = true;

}
