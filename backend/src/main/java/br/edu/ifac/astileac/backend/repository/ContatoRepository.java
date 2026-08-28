package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Contato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoRepository extends JpaRepository<Contato, Long> {

	List<Contato> findByProfissionalId(Long profissionalId);

}
