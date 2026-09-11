package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.LogAcesso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAcessoRepository extends JpaRepository<LogAcesso, Long> {

	// "Tudo que aconteceu com este registro" -- coberto pelo indice
	// idx_log_acesso_registro criado na V4.
	Page<LogAcesso> findByEntidadeAndRegistroId(String entidade, Long registroId, Pageable pageable);

}
