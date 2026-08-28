package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.VinculoEmpregaticio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoEmpregaticioRepository extends JpaRepository<VinculoEmpregaticio, Long> {

	List<VinculoEmpregaticio> findByProfissionalId(Long profissionalId);

}
