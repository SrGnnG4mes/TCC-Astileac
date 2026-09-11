package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.AreaAtuacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaAtuacaoRepository extends JpaRepository<AreaAtuacao, Long> {

	Optional<AreaAtuacao> findByNome(String nome);

	boolean existsByNome(String nome);

}
