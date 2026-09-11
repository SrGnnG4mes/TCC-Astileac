package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de acesso a dado pessoal (RF11, LGPD Art. 46).
 *
 * Tabela so de escrita pela aplicacao: nunca se altera nem se apaga um
 * registro de log -- um historico que pode ser editado nao serve como
 * historico.
 */
@Entity
@Table(name = "log_acesso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogAcesso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "data_hora", nullable = false)
	private LocalDateTime dataHora;

	/**
	 * Login de quem fez a operacao. Enquanto o RF09 (autenticacao) nao
	 * existe, fica "nao-autenticado": o sistema ainda nao tem como
	 * saber quem e. E uma linha para trocar no Passo 6.
	 */
	@Column(nullable = false, length = 100)
	private String autor;

	/** Endereco de origem da requisicao. */
	@Column(length = 45)
	private String origem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AcaoLog acao;

	@Column(nullable = false, length = 50)
	private String entidade;

	@Column(name = "registro_id")
	private Long registroId;

	@Column(length = 255)
	private String detalhe;

}
