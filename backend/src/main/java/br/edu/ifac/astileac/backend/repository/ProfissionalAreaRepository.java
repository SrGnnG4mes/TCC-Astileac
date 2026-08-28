package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.ProfissionalArea;
import br.edu.ifac.astileac.backend.entity.ProfissionalAreaId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfissionalAreaRepository extends JpaRepository<ProfissionalArea, ProfissionalAreaId> {

	// Areas de um profissional.
	List<ProfissionalArea> findByIdProfissionalId(Long profissionalId);

	// Profissionais de uma area -- base do RF08 (filtro por area de atuacao).
	List<ProfissionalArea> findByIdAreaAtuacaoId(Long areaAtuacaoId);

}
