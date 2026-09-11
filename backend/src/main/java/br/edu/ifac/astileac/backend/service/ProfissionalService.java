package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.ProfissionalRequestDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResponseDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResumoDTO;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.CpfJaCadastradoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio do cadastro de profissional (RF01). CRUD completo:
 * criar, listar, buscar por id, atualizar e remover.
 */
@Service
@RequiredArgsConstructor
public class ProfissionalService {

	private final ProfissionalRepository profissionalRepository;

	@Transactional
	public ProfissionalResponseDTO criar(ProfissionalRequestDTO dto) {
		if (profissionalRepository.existsByCpf(dto.cpf())) {
			throw new CpfJaCadastradoException();
		}

		Profissional profissional = new Profissional();
		profissional.setNome(dto.nome());
		profissional.setCpf(dto.cpf());
		profissional.setDataNascimento(dto.dataNascimento());
		profissional.setDataAssociacao(dto.dataAssociacao());
		profissional.setAtivo(true);

		return toResponseDTO(profissionalRepository.save(profissional));
	}

	/**
	 * Listagem paginada. Devolve o resumo (CPF mascarado), nunca a
	 * ficha completa: uma tela de busca nao precisa do CPF inteiro de
	 * todo mundo, e sem paginacao uma unica chamada traria a base toda.
	 */
	@Transactional(readOnly = true)
	public Page<ProfissionalResumoDTO> listar(Pageable pageable) {
		return profissionalRepository.findAll(pageable).map(this::toResumoDTO);
	}

	@Transactional(readOnly = true)
	public ProfissionalResponseDTO buscarPorId(Long id) {
		return toResponseDTO(buscarEntidadePorId(id));
	}

	@Transactional
	public ProfissionalResponseDTO atualizar(Long id, ProfissionalRequestDTO dto) {
		Profissional profissional = buscarEntidadePorId(id);

		boolean cpfMudou = !profissional.getCpf().equals(dto.cpf());
		if (cpfMudou && profissionalRepository.existsByCpf(dto.cpf())) {
			throw new CpfJaCadastradoException();
		}

		profissional.setNome(dto.nome());
		profissional.setCpf(dto.cpf());
		profissional.setDataNascimento(dto.dataNascimento());
		profissional.setDataAssociacao(dto.dataAssociacao());

		return toResponseDTO(profissionalRepository.save(profissional));
	}

	@Transactional
	public void remover(Long id) {
		profissionalRepository.delete(buscarEntidadePorId(id));
	}

	private Profissional buscarEntidadePorId(Long id) {
		return profissionalRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Profissional nao encontrado: id " + id));
	}

	private ProfissionalResumoDTO toResumoDTO(Profissional profissional) {
		return new ProfissionalResumoDTO(
				profissional.getId(),
				profissional.getNome(),
				mascararCpf(profissional.getCpf()),
				profissional.getAtivo()
		);
	}

	/**
	 * 12345678901 -> ***.***.789-01. Mantem os digitos finais, que
	 * bastam para distinguir dois homonimos numa lista, e esconde o
	 * restante.
	 */
	private String mascararCpf(String cpf) {
		if (cpf == null || cpf.length() != 11) {
			return "***.***.***-**";
		}
		return "***.***." + cpf.substring(6, 9) + "-" + cpf.substring(9);
	}

	private ProfissionalResponseDTO toResponseDTO(Profissional profissional) {
		return new ProfissionalResponseDTO(
				profissional.getId(),
				profissional.getNome(),
				profissional.getCpf(),
				profissional.getDataNascimento(),
				profissional.getDataAssociacao(),
				profissional.getAtivo()
		);
	}

}
