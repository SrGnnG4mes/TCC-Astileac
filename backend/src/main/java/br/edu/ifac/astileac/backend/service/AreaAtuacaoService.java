package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.AreaAtuacaoRequestDTO;
import br.edu.ifac.astileac.backend.dto.AreaAtuacaoResponseDTO;
import br.edu.ifac.astileac.backend.entity.AreaAtuacao;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.AreaAtuacaoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Areas de atuacao: a tabela de dominio que o RF04 usa. Lista curta e
 * estavel (cinco na carga inicial), entao a listagem nao e paginada.
 */
@Service
@RequiredArgsConstructor
public class AreaAtuacaoService {

	private final AreaAtuacaoRepository areaAtuacaoRepository;

	@Transactional
	public AreaAtuacaoResponseDTO criar(AreaAtuacaoRequestDTO dto) {
		String nome = normalizar(dto.nome());
		if (areaAtuacaoRepository.existsByNome(nome)) {
			throw new ConflitoException("Já existe uma área de atuação com este nome.");
		}

		AreaAtuacao area = new AreaAtuacao();
		area.setNome(nome);
		area.setDescricao(dto.descricao());

		return toResponseDTO(areaAtuacaoRepository.save(area));
	}

	@Transactional(readOnly = true)
	public List<AreaAtuacaoResponseDTO> listar() {
		return areaAtuacaoRepository.findAll(Sort.by("nome")).stream()
				.map(this::toResponseDTO)
				.toList();
	}

	@Transactional(readOnly = true)
	public AreaAtuacaoResponseDTO buscarPorId(Long id) {
		return toResponseDTO(buscarEntidade(id));
	}

	@Transactional
	public AreaAtuacaoResponseDTO atualizar(Long id, AreaAtuacaoRequestDTO dto) {
		AreaAtuacao area = buscarEntidade(id);
		String nome = normalizar(dto.nome());

		boolean nomeMudou = !area.getNome().equals(nome);
		if (nomeMudou && areaAtuacaoRepository.existsByNome(nome)) {
			throw new ConflitoException("Já existe uma área de atuação com este nome.");
		}

		area.setNome(nome);
		area.setDescricao(dto.descricao());

		return toResponseDTO(areaAtuacaoRepository.save(area));
	}

	/**
	 * Remover uma area que tenha profissionais vinculados esbarra na
	 * chave estrangeira e vira 409, tratado no GlobalExceptionHandler.
	 * E o comportamento desejado: a area so sai se ninguem atuar nela.
	 */
	@Transactional
	public void remover(Long id) {
		areaAtuacaoRepository.delete(buscarEntidade(id));
	}

	/**
	 * Usado tambem pelo ProfissionalAreaService, para nao repetir a
	 * mensagem de "area nao encontrada".
	 */
	@Transactional(readOnly = true)
	public AreaAtuacao buscarEntidade(Long id) {
		return areaAtuacaoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Área de atuação não encontrada: id " + id));
	}

	/**
	 * Nome guardado em minusculas, seguindo a carga inicial da V2. Sem
	 * isso, "Juridica" e "juridica" conviveriam como areas distintas e
	 * o filtro do RF08 passaria a depender de como cada um digitou. A
	 * apresentacao (maiuscula inicial) fica com o front-end -- mesma
	 * convencao ja adotada para CPF e CEP: guardar limpo, formatar na
	 * exibicao.
	 */
	private String normalizar(String nome) {
		return nome.trim().toLowerCase();
	}

	private AreaAtuacaoResponseDTO toResponseDTO(AreaAtuacao area) {
		return new AreaAtuacaoResponseDTO(area.getId(), area.getNome(), area.getDescricao());
	}

}
