package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByLogin(String login);

	boolean existsByLogin(String login);

}
