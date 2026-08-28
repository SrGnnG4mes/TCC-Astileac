package br.edu.ifac.astileac.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chave composta de profissional_area: profissional_id + area_atuacao_id.
 * Precisa implementar Serializable e equals/hashCode (exigencia do JPA
 * para chaves compostas via @EmbeddedId).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalAreaId implements Serializable {

	@Column(name = "profissional_id")
	private Long profissionalId;

	@Column(name = "area_atuacao_id")
	private Long areaAtuacaoId;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ProfissionalAreaId that)) {
			return false;
		}
		return Objects.equals(profissionalId, that.profissionalId)
				&& Objects.equals(areaAtuacaoId, that.areaAtuacaoId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(profissionalId, areaAtuacaoId);
	}

}
