package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByLogin(String login);

	boolean existsByLogin(String login);

	/**
	 * Conta administradores.
	 *
	 * Serve a regra que impede a associacao de se trancar para fora do
	 * proprio sistema: nenhuma operacao pode deixar zero contas com
	 * perfil ADMIN, porque so um administrador mexe na conta de outro.
	 */
	long countByPerfil(String perfil);

	/**
	 * Usado pelo expurgo da LGPD: antes de apagar o profissional e
	 * preciso apagar a conta que aponta para ele, senao a chave
	 * estrangeira impede a exclusao.
	 */
	Optional<Usuario> findByProfissionalId(Long profissionalId);
}
