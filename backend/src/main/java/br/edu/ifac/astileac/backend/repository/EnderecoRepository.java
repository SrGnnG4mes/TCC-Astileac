package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Endereco;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

	// Como o mapeamento e unidirecional (ver package-info do pacote entity),
	// esta e a unica forma de buscar o endereco de um profissional.
	Optional<Endereco> findByProfissionalId(Long profissionalId);

}
