package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Formacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormacaoRepository extends JpaRepository<Formacao, Long> {

	List<Formacao> findByProfissionalId(Long profissionalId);

}
