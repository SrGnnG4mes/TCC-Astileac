package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.VinculoEmpregaticioRequestDTO;
import br.edu.ifac.astileac.backend.dto.VinculoEmpregaticioResponseDTO;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.VinculoEmpregaticio;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.VinculoEmpregaticioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vinculos empregaticios do profissional (RF07). Relacao 1:N.
 *
 * Guarda o HISTORICO, nao so o vinculo vigente: um profissional pode
 * ter varios vinculos encerrados e mais de um em andamento ao mesmo
 * tempo (e comum atuar em duas instituicoes).
 */
@Service
@RequiredArgsConstructor
public class VinculoEmpregaticioService {

	private final VinculoEmpregaticioRepository vinculoEmpregaticioRepository;
	private final ProfissionalService profissionalService;

	@Transactional
	public VinculoEmpregaticioResponseDTO criar(Long profissionalId, VinculoEmpregaticioRequestDTO dto) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);
		validarPeriodo(dto);

		VinculoEmpregaticio vinculo = new VinculoEmpregaticio();
		vinculo.setProfissional(profissional);
		aplicar(vinculo, dto);

		return toResponseDTO(vinculoEmpregaticioRepository.save(vinculo));
	}

	@Transactional(readOnly = true)
	public List<VinculoEmpregaticioResponseDTO> listar(Long profissionalId) {
		profissionalService.buscarEntidade(profissionalId);
		return vinculoEmpregaticioRepository.findByProfissionalId(profissionalId).stream()
				.map(this::toResponseDTO)
				.toList();
	}

	@Transactional
	public VinculoEmpregaticioResponseDTO atualizar(
			Long profissionalId, Long vinculoId, VinculoEmpregaticioRequestDTO dto) {
		profissionalService.buscarEntidade(profissionalId);
		validarPeriodo(dto);

		VinculoEmpregaticio vinculo = buscarEntidade(profissionalId, vinculoId);
		aplicar(vinculo, dto);

		return toResponseDTO(vinculoEmpregaticioRepository.save(vinculo));
	}

	@Transactional
	public void remover(Long profissionalId, Long vinculoId) {
		profissionalService.buscarEntidade(profissionalId);
		vinculoEmpregaticioRepository.delete(buscarEntidade(profissionalId, vinculoId));
	}

	private void aplicar(VinculoEmpregaticio vinculo, VinculoEmpregaticioRequestDTO dto) {
		vinculo.setInstituicao(dto.instituicao().trim());
		vinculo.setCargo(dto.cargo().trim());
		vinculo.setTipoVinculo(dto.tipoVinculo().trim());
		vinculo.setDataInicio(dto.dataInicio());
		vinculo.setDataFim(dto.dataFim());
		// Derivado, nunca recebido: data_fim nula = em andamento.
		vinculo.setAtual(dto.dataFim() == null);
	}

	private void validarPeriodo(VinculoEmpregaticioRequestDTO dto) {
		if (dto.dataFim() != null && dto.dataFim().isBefore(dto.dataInicio())) {
			throw new RegraDeNegocioException("A data de término não pode ser anterior à data de início.");
		}
	}

	private VinculoEmpregaticio buscarEntidade(Long profissionalId, Long vinculoId) {
		return vinculoEmpregaticioRepository.findById(vinculoId)
				.filter(vinculo -> vinculo.getProfissional().getId().equals(profissionalId))
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Vínculo não encontrado para este profissional: id " + vinculoId));
	}

	private VinculoEmpregaticioResponseDTO toResponseDTO(VinculoEmpregaticio vinculo) {
		return new VinculoEmpregaticioResponseDTO(
				vinculo.getId(),
				vinculo.getInstituicao(),
				vinculo.getCargo(),
				vinculo.getTipoVinculo(),
				vinculo.getDataInicio(),
				vinculo.getDataFim(),
				vinculo.getAtual()
		);
	}

}
