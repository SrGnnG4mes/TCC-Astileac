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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Convite de cadastro (RF12).
 *
 * A diretoria emite depois de confirmar o pagamento da associacao. O
 * convite chega por e-mail e serve uma vez so -- e ele que liga "quem
 * esta se cadastrando" a "quem a associacao reconheceu como associado".
 *
 * Nunca guarda o token em si, so o hash: e credencial portadora, igual
 * a senha.
 */
@Entity
@Table(name = "convite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Convite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String email;

	/**
	 * CHAR(64) no banco. O Hibernate mapeia String para VARCHAR por
	 * padrao, e a validacao do schema so passa com o tipo declarado --
	 * mesmo caso do endereco.uf.
	 */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "data_emissao", nullable = false)
	private LocalDateTime dataEmissao;

	@Column(name = "data_expiracao", nullable = false)
	private LocalDateTime dataExpiracao;

	/** Nulo enquanto nao usado: e o que torna o convite de uso unico. */
	@Column(name = "data_consumo")
	private LocalDateTime dataConsumo;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profissional_id", unique = true)
	private Profissional profissional;

	@Column(name = "emitido_por", nullable = false, length = 100)
	private String emitidoPor;

	public boolean estaConsumido() {
		return dataConsumo != null;
	}

	public boolean estaExpirado() {
		return LocalDateTime.now().isAfter(dataExpiracao);
	}

	public boolean estaValido() {
		return !estaConsumido() && !estaExpirado();
	}
}
