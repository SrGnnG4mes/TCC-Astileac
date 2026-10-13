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
		profissional.setLattesUrl(vazioComoNulo(dto.lattesUrl()));

		return toResponseDTO(profissionalRepository.save(profissional));
	}

	/**
	 * Consulta paginada com filtros opcionais (RF08) -- o caso de uso
	 * central: a associacao recebe uma demanda e precisa achar quem
	 * atende aquela area.
	 *
	 * Devolve o resumo (CPF mascarado), nunca a ficha completa: quem
	 * atende o telefone precisa localizar a pessoa certa e so depois
	 * abrir o cadastro dela. Sao duas etapas, e a primeira expoe o
	 * minimo.
	 *
	 * Filtro nulo significa "nao filtre por isso", entao chamar sem
	 * parametro nenhum devolve a listagem geral -- nao existem dois
	 * caminhos de codigo para manter.
	 */
	@Transactional(readOnly = true)
	public Page<ProfissionalResumoDTO> consultar(
			String nome, Boolean ativo, String area, Pageable pageable) {
		return profissionalRepository
				.consultar(normalizar(nome), ativo, normalizar(area), pageable)
				.map(this::toResumoDTO);
	}

	/**
	 * Texto em branco vale como filtro ausente: uma caixa de busca
	 * vazia no front-end nao pode virar um LIKE por string vazia. O
	 * nome da area desce para minusculas porque e assim que ela esta
	 * guardada (ver AreaAtuacaoService).
	 */
	private String normalizar(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim().toLowerCase();
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
		profissional.setLattesUrl(vazioComoNulo(dto.lattesUrl()));

		return toResponseDTO(profissionalRepository.save(profissional));
	}

	/**
	 * Desativa ou reativa um profissional, decisao da diretoria sobre o
	 * quadro de associados.
	 *
	 * SUBSTITUI A EXCLUSAO ADMINISTRATIVA QUE EXISTIA AQUI, por decisao
	 * da diretoria. Sao duas coisas diferentes que estavam confundidas:
	 *
	 *  - o TITULAR que pede eliminacao exerce o Art. 18, e ai apaga
	 *    mesmo -- e o RF10, com janela de arrependimento e expurgo;
	 *  - a DIRETORIA que tira alguem do quadro (saiu, parou de pagar,
	 *    faleceu) esta administrando a associacao, nao atendendo a um
	 *    direito. Apagar aqui destruiria o registro de que a pessoa FOI
	 *    associada, que e justamente o que a entidade precisa guardar.
	 *
	 * Por isso nao ha mais exclusao administrativa: remover em definitivo
	 * e ato de protecao de dados, nao de administracao do quadro.
	 */
	@Transactional
	public ProfissionalResponseDTO alterarSituacao(Long id, boolean ativo) {
		Profissional profissional = buscarEntidadePorId(id);
		profissional.setAtivo(ativo);
		return toResponseDTO(profissionalRepository.save(profissional));
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

	/**
	 * Campo de texto opcional vazio vira nulo.
	 *
	 * O formulario manda string vazia quando o usuario nao preenche, e
	 * gravar "" faria o banco distinguir "nao informou" de "informou
	 * nada" -- duas formas de dizer a mesma coisa, que depois exigiriam
	 * dois testes em toda consulta.
	 */
	private String vazioComoNulo(String valor) {
		return (valor == null || valor.isBlank()) ? null : valor.trim();
	}

	private ProfissionalResponseDTO toResponseDTO(Profissional profissional) {
		return new ProfissionalResponseDTO(
				profissional.getId(),
				profissional.getNome(),
				profissional.getCpf(),
				profissional.getDataNascimento(),
				profissional.getDataAssociacao(),
				profissional.getAtivo(),
				profissional.getDataExclusaoSolicitada(),
				profissional.getLattesUrl()
		);
	}

}
