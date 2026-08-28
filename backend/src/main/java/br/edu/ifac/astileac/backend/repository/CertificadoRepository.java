package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Certificado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

	List<Certificado> findByProfissionalId(Long profissionalId);

	List<Certificado> findByFormacaoId(Long formacaoId);

}
