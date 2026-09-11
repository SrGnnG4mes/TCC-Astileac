package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Contato;
import br.edu.ifac.astileac.backend.entity.TipoContato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoRepository extends JpaRepository<Contato, Long> {

	List<Contato> findByProfissionalId(Long profissionalId);

	/**
	 * Usado para manter a regra de um unico contato principal por tipo:
	 * ao marcar um novo principal, os anteriores do mesmo tipo deixam
	 * de ser.
	 */
	List<Contato> findByProfissionalIdAndTipoAndPrincipalTrue(Long profissionalId, TipoContato tipo);

}
