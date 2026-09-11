package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.ContatoRequestDTO;
import br.edu.ifac.astileac.backend.dto.ContatoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Contato;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.TipoContato;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.ContatoRepository;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contatos do profissional (RF03). Relacao 1:N -- varios e-mails e/ou
 * telefones por pessoa.
 */
@Service
@RequiredArgsConstructor
public class ContatoService {

	/** Suficiente para pegar erro de digitacao; nao pretende validar RFC 5322. */
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

	/** Telefone brasileiro: DDD + 8 digitos (fixo) ou DDD + 9 digitos (celular). */
	private static final Pattern TELEFONE = Pattern.compile("^\\d{10,11}$");

	private final ContatoRepository contatoRepository;
	private final ProfissionalService profissionalService;

	@Transactional
	public ContatoResponseDTO criar(Long profissionalId, ContatoRequestDTO dto) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);
		validarValor(dto);

		Contato contato = new Contato();
		contato.setProfissional(profissional);
		contato.setTipo(dto.tipo());
		contato.setValor(dto.valor());
		contato.setPrincipal(Boolean.TRUE.equals(dto.principal()));

		if (contato.getPrincipal()) {
			desmarcarPrincipalAnterior(profissionalId, dto.tipo(), null);
		}

		return toResponseDTO(contatoRepository.save(contato));
	}

	@Transactional(readOnly = true)
	public List<ContatoResponseDTO> listar(Long profissionalId) {
		profissionalService.buscarEntidade(profissionalId);
		return contatoRepository.findByProfissionalId(profissionalId).stream()
				.map(this::toResponseDTO)
				.toList();
	}

	@Transactional
	public ContatoResponseDTO atualizar(Long profissionalId, Long contatoId, ContatoRequestDTO dto) {
		profissionalService.buscarEntidade(profissionalId);
		validarValor(dto);

		Contato contato = buscarEntidade(profissionalId, contatoId);
		contato.setTipo(dto.tipo());
		contato.setValor(dto.valor());
		contato.setPrincipal(Boolean.TRUE.equals(dto.principal()));

		if (contato.getPrincipal()) {
			desmarcarPrincipalAnterior(profissionalId, dto.tipo(), contatoId);
		}

		return toResponseDTO(contatoRepository.save(contato));
	}

	@Transactional
	public void remover(Long profissionalId, Long contatoId) {
		profissionalService.buscarEntidade(profissionalId);
		contatoRepository.delete(buscarEntidade(profissionalId, contatoId));
	}

	/**
	 * Garante que o contato pertence mesmo ao profissional da URL.
	 * Sem isso, /profissionais/1/contatos/99 conseguiria alterar um
	 * contato do profissional 2 -- o contatoId sozinho nao prova nada.
	 */
	private Contato buscarEntidade(Long profissionalId, Long contatoId) {
		return contatoRepository.findById(contatoId)
				.filter(contato -> contato.getProfissional().getId().equals(profissionalId))
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Contato nao encontrado para este profissional: id " + contatoId));
	}

	/**
	 * Um unico contato principal por tipo: o profissional pode ter um
	 * e-mail principal e um telefone principal, nunca dois do mesmo
	 * tipo. Marcar um novo desmarca o anterior, em vez de recusar a
	 * operacao -- e o que a pessoa quis dizer ao marcar.
	 */
	private void desmarcarPrincipalAnterior(Long profissionalId, TipoContato tipo, Long idQueFicaPrincipal) {
		contatoRepository.findByProfissionalIdAndTipoAndPrincipalTrue(profissionalId, tipo).stream()
				.filter(anterior -> !anterior.getId().equals(idQueFicaPrincipal))
				.forEach(anterior -> {
					anterior.setPrincipal(false);
					contatoRepository.save(anterior);
				});
	}

	private void validarValor(ContatoRequestDTO dto) {
		boolean valido = switch (dto.tipo()) {
			case EMAIL -> EMAIL.matcher(dto.valor()).matches();
			case TELEFONE -> TELEFONE.matcher(dto.valor()).matches();
		};

		if (!valido) {
			throw new RegraDeNegocioException(switch (dto.tipo()) {
				case EMAIL -> "valor: informe um e-mail valido para o tipo EMAIL";
				case TELEFONE -> "valor: telefone deve ter 10 ou 11 digitos com DDD, sem pontuacao";
			});
		}
	}

	private ContatoResponseDTO toResponseDTO(Contato contato) {
		return new ContatoResponseDTO(
				contato.getId(),
				contato.getTipo(),
				contato.getValor(),
				contato.getPrincipal()
		);
	}

}
