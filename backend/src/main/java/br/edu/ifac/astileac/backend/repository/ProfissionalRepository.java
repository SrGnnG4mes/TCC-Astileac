package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Profissional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

	Optional<Profissional> findByCpf(String cpf);

	boolean existsByCpf(String cpf);

}
