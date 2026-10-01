package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Convite;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConviteRepository extends JpaRepository<Convite, Long> {

	Optional<Convite> findByTokenHash(String tokenHash);

	/** Pendentes primeiro, para a diretoria ver o que ainda nao virou cadastro. */
	List<Convite> findByDataConsumoIsNullOrderByDataEmissaoDesc();

	Optional<Convite> findByProfissionalId(Long profissionalId);

	/**
	 * Marca o convite como consumido somente se ainda nao estiver.
	 *
	 * E uma atualizacao condicional de proposito, e nao um "ler, checar,
	 * gravar" no service: o token e credencial portadora e pode chegar
	 * duas vezes ao mesmo tempo -- duplo clique, requisicao repetida.
	 * Entre a leitura e a gravacao haveria janela para os dois caminhos
	 * passarem pela checagem, e o convite de uso unico criaria dois
	 * cadastros. Aqui quem garante a exclusividade e o banco: a segunda
	 * chamada afeta zero linhas.
	 *
	 * @return quantas linhas foram afetadas -- 1 se este chamador ganhou
	 *         a corrida, 0 se o convite ja havia sido consumido
	 */
	@Modifying
	@Query("""
		UPDATE Convite c
		SET c.dataConsumo = :agora
		WHERE c.id = :id AND c.dataConsumo IS NULL
		""")
	int marcarConsumido(@Param("id") Long id, @Param("agora") LocalDateTime agora);
}
