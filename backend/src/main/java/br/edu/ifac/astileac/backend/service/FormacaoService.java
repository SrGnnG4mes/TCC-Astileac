package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.FormacaoRequestDTO;
import br.edu.ifac.astileac.backend.dto.FormacaoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Formacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.FormacaoRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Formacoes do profissional (RF05). Relacao 1:N.
 */
@Service
@RequiredArgsConstructor
public class FormacaoService {

	private final FormacaoRepository formacaoRepository;
	private final ProfissionalService profissionalService;

	@Transactional
	public FormacaoResponseDTO criar(Long profissionalId, FormacaoRequestDTO dto) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);
		validarAnoConclusao(profissional, dto.anoConclusao());

		Formacao formacao = new Formacao();
		formacao.setProfissional(profissional);
		aplicar(formacao, dto);

		return toResponseDTO(formacaoRepository.save(formacao));
	}

	@Transactional(readOnly = true)
	public List<FormacaoResponseDTO> listar(Long profissionalId) {
		profissionalService.buscarEntidade(profissionalId);
		return formacaoRepository.findByProfissionalId(profissionalId).stream()
				.map(this::toResponseDTO)
				.toList();
	}

	@Transactional
	public FormacaoResponseDTO atualizar(Long profissionalId, Long formacaoId, FormacaoRequestDTO dto) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);
		validarAnoConclusao(profissional, dto.anoConclusao());

		Formacao formacao = buscarEntidade(profissionalId, formacaoId);
		aplicar(formacao, dto);

		return toResponseDTO(formacaoRepository.save(formacao));
	}

	@Transactional
	public void remover(Long profissionalId, Long formacaoId) {
		profissionalService.buscarEntidade(profissionalId);
		formacaoRepository.delete(buscarEntidade(profissionalId, formacaoId));
	}

	/**
	 * Usado tambem pelo CertificadoService, que precisa conferir se a
	 * formacao vinculada a um certificado e do mesmo profissional.
	 */
	@Transactional(readOnly = true)
	public Formacao buscarEntidade(Long profissionalId, Long formacaoId) {
		return formacaoRepository.findById(formacaoId)
				.filter(formacao -> formacao.getProfissional().getId().equals(profissionalId))
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Formação não encontrada para este profissional: id " + formacaoId));
	}

	private void aplicar(Formacao formacao, FormacaoRequestDTO dto) {
		formacao.setNivel(dto.nivel().trim());
		formacao.setCurso(dto.curso().trim());
		formacao.setInstituicao(dto.instituicao().trim());
		formacao.setAnoConclusao(dto.anoConclusao());
	}

	/**
	 * Duas checagens que o Bean Validation nao alcanca: a primeira
	 * depende da data de hoje, a segunda de um dado do profissional.
	 */
	private void validarAnoConclusao(Profissional profissional, Integer anoConclusao) {
		int anoAtual = LocalDate.now().getYear();
		if (anoConclusao > anoAtual) {
			throw new RegraDeNegocioException(
					"O ano de conclusão não pode estar no futuro.");
		}

		int anoNascimento = profissional.getDataNascimento().getYear();
		if (anoConclusao < anoNascimento) {
			throw new RegraDeNegocioException(
					"O ano de conclusão não pode ser anterior ao ano de nascimento.");
		}
	}

	private FormacaoResponseDTO toResponseDTO(Formacao formacao) {
		return new FormacaoResponseDTO(
				formacao.getId(),
				formacao.getNivel(),
				formacao.getCurso(),
				formacao.getInstituicao(),
				formacao.getAnoConclusao()
		);
	}

}
