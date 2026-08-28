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
 * Relacao 1:N com profissional; ligacao com formacao e opcional
 * (formacao_id aceita nulo no schema). arquivo_path guarda so o
 * caminho do arquivo enviado -- o binario nunca vai para o banco
 * (uploads em ./uploads, fora do Git).
 */
@Entity
@Table(name = "certificado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Certificado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "formacao_id")
	private Formacao formacao;

	@Column(nullable = false, length = 150)
	private String titulo;

	@Column(name = "instituicao_emissora", nullable = false, length = 150)
	private String instituicaoEmissora;

	@Column(name = "carga_horaria", nullable = false)
	private Integer cargaHoraria;

	@Column(name = "data_emissao", nullable = false)
	private LocalDate dataEmissao;

	@Column(name = "arquivo_path", nullable = false, length = 255)
	private String arquivoPath;

}
