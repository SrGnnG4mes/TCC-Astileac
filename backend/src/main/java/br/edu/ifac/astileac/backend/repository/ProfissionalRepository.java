package br.edu.ifac.astileac.backend.repository;

import br.edu.ifac.astileac.backend.entity.Profissional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	/**
	 * Consulta do RF08 -- o caso de uso que justifica o sistema: a
	 * associacao recebe "preciso de um interprete para audiencia
	 * judicial" e precisa responder rapido.
	 *
	 * JPQL explicito em vez de Specification: e uma consulta so, da
	 * para ler de cima a baixo e defender numa banca. Specification
	 * seria mais flexivel e bem menos legivel.
	 *
	 * Cada filtro e opcional pelo padrao ":param IS NULL OR ...", em
	 * que nulo significa "nao filtre por isso". O CAST existe porque,
	 * sem ele, o Hibernate nao consegue inferir o tipo do parametro
	 * quando ele chega nulo.
	 *
	 * O filtro de area usa EXISTS, e nao JOIN, de proposito: com JOIN,
	 * um profissional que atua em duas areas apareceria duplicado e
	 * estragaria a contagem da paginacao.
	 *
	 * A exclusao de quem pediu eliminacao nao e opcional aqui: e a
	 * mesma regra de bloqueio da listagem comum.
	 */
	@Query("""
			SELECT p FROM Profissional p
			WHERE p.dataExclusaoSolicitada IS NULL
			  AND (CAST(:nome AS string) IS NULL
			       OR LOWER(p.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
			  AND (:ativo IS NULL OR p.ativo = :ativo)
			  AND (CAST(:area AS string) IS NULL
			       OR EXISTS (SELECT 1 FROM ProfissionalArea pa
			                  WHERE pa.profissional = p
			                    AND pa.areaAtuacao.nome = CAST(:area AS string)))
			""")
	Page<Profissional> consultar(
			@Param("nome") String nome,
			@Param("ativo") Boolean ativo,
			@Param("area") String area,
			Pageable pageable);

}
