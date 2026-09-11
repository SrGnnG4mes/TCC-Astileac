package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Profissional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

	Optional<Profissional> findByCpf(String cpf);

	boolean existsByCpf(String cpf);

	/**
	 * Listagem normal: quem pediu exclusao fica BLOQUEADO durante a
	 * janela de arrependimento -- some das consultas, mas continua no
	 * banco para o caso de mudar de ideia (LGPD Art. 18, IV).
	 */
	Page<Profissional> findByDataExclusaoSolicitadaIsNull(Pageable pageable);

	/** Cadastros cuja janela de arrependimento ja venceu. */
	List<Profissional> findByDataExclusaoSolicitadaBefore(LocalDateTime limite);

}
