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
import java.time.LocalDateTime;
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

	/**
	 * Caminho do arquivo digitalizado, relativo a pasta de uploads.
	 *
	 * Opcional desde a migration V5: o certificado e registrado antes de
	 * o documento ser digitalizado. A anotacao dizia nullable = false
	 * mesmo depois da V5 -- nao quebrava, porque com Bean Validation
	 * presente o Hibernate desliga a propria checagem de nulidade, e o
	 * ddl-auto=validate nao confere nulidade de coluna. Era anotacao
	 * mentindo sobre o banco.
	 */
	@Column(name = "arquivo_path", length = 255)
	private String arquivoPath;

	/**
	 * Reconhecimento do documento pela diretoria (RF13).
	 *
	 * Nulo = ainda nao reconhecido. O timestamp faz as vezes de
	 * marcador; um booleano ao lado seria estado duplicado.
	 *
	 * Anexar um arquivo novo LIMPA os dois campos: sem isso, bastaria
	 * enviar o documento A, esperar o reconhecimento e trocar pelo
	 * documento B, que ficaria validado sem nunca ter sido visto.
	 */
	@Column(name = "validado_em")
	private LocalDateTime validadoEm;

	@Column(name = "validado_por", length = 100)
	private String validadoPor;

	public boolean temDocumento() {
		return arquivoPath != null;
	}

	public boolean estaValidado() {
		return validadoEm != null;
	}

}
