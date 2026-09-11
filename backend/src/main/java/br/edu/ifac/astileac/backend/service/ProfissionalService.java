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
		// Quem pediu exclusao fica bloqueado durante a janela de
		// arrependimento: nao aparece nas consultas da associacao.
		return profissionalRepository.findByDataExclusaoSolicitadaIsNull(pageable).map(this::toResumoDTO);
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

	/**
	 * Busca a entidade para uso de OUTROS services (endereco, contatos,
	 * formacoes...), que precisam garantir que o profissional existe
	 * antes de mexer nos dados filhos. Mantem a mensagem de "nao
	 * encontrado" num lugar so, em vez de repetida em cada cadastro.
	 *
	 * Devolver a entidade aqui nao fere a regra do projeto: o que nao
	 * pode e entidade JPA chegar ao controller. Entre services, e o
	 * objeto de trabalho normal.
	 */
	@Transactional(readOnly = true)
	public Profissional buscarEntidade(Long id) {
		return buscarEntidadePorId(id);
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
				profissional.getAtivo(),
				profissional.getDataExclusaoSolicitada()
		);
	}

}
