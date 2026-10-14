package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.EnderecoRequestDTO;
import br.edu.ifac.astileac.backend.dto.EnderecoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Endereco;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.EnderecoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endereco do profissional (RF02). Relacao 1:1, entao nao ha "listar"
 * nem "criar varios": o profissional tem um endereco ou nenhum.
 */
@Service
@RequiredArgsConstructor
public class EnderecoService {

	private final EnderecoRepository enderecoRepository;
	private final ProfissionalService profissionalService;

	/**
	 * Cria o endereco ou substitui o existente. Como so cabe um por
	 * profissional, a operacao e idempotente -- chamar duas vezes com
	 * os mesmos dados deixa o banco no mesmo estado.
	 */
	@Transactional
	public EnderecoResponseDTO salvar(Long profissionalId, EnderecoRequestDTO dto) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);

		Endereco endereco = enderecoRepository.findByProfissionalId(profissionalId)
				.orElseGet(() -> {
					Endereco novo = new Endereco();
					novo.setProfissional(profissional);
					return novo;
				});

		endereco.setCep(dto.cep());
		endereco.setLogradouro(dto.logradouro());
		endereco.setNumero(dto.numero());
		endereco.setComplemento(dto.complemento());
		endereco.setBairro(dto.bairro());
		endereco.setCidade(dto.cidade());
		endereco.setUf(dto.uf());

		return toResponseDTO(enderecoRepository.save(endereco));
	}

	@Transactional(readOnly = true)
	public EnderecoResponseDTO buscar(Long profissionalId) {
		profissionalService.buscarEntidade(profissionalId);
		return toResponseDTO(buscarEntidade(profissionalId));
	}

	@Transactional
	public void remover(Long profissionalId) {
		profissionalService.buscarEntidade(profissionalId);
		enderecoRepository.delete(buscarEntidade(profissionalId));
	}

	private Endereco buscarEntidade(Long profissionalId) {
		return enderecoRepository.findByProfissionalId(profissionalId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Profissional não possui endereço cadastrado: id " + profissionalId));
	}

	private EnderecoResponseDTO toResponseDTO(Endereco endereco) {
		return new EnderecoResponseDTO(
				endereco.getId(),
				endereco.getCep(),
				endereco.getLogradouro(),
				endereco.getNumero(),
				endereco.getComplemento(),
				endereco.getBairro(),
				endereco.getCidade(),
				endereco.getUf()
		);
	}

}
