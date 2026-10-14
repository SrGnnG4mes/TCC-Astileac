package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.ProfissionalAreaRequestDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalAreaResponseDTO;
import br.edu.ifac.astileac.backend.entity.AreaAtuacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.ProfissionalArea;
import br.edu.ifac.astileac.backend.entity.ProfissionalAreaId;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.ProfissionalAreaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vinculo entre profissional e area de atuacao (RF04) -- o ponto
 * central do modelo, segundo o CLAUDE.md.
 *
 * E a unica relacao N:N do sistema, e a unica com chave composta. A
 * associacao carrega um atributo proprio (data_inicio), o que e
 * exatamente o motivo de ser uma tabela e nao um simples vinculo:
 * "desde quando" nao pertence nem ao profissional nem a area.
 */
@Service
@RequiredArgsConstructor
public class ProfissionalAreaService {

	private final ProfissionalAreaRepository profissionalAreaRepository;
	private final ProfissionalService profissionalService;
	private final AreaAtuacaoService areaAtuacaoService;

	@Transactional
	public ProfissionalAreaResponseDTO associar(Long profissionalId, ProfissionalAreaRequestDTO dto) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);
		AreaAtuacao area = areaAtuacaoService.buscarEntidade(dto.areaAtuacaoId());

		ProfissionalAreaId chave = new ProfissionalAreaId(profissionalId, dto.areaAtuacaoId());
		if (profissionalAreaRepository.existsById(chave)) {
			throw new ConflitoException("Este profissional já atua nesta área.");
		}

		ProfissionalArea vinculo = new ProfissionalArea();
		vinculo.setProfissional(profissional);
		vinculo.setAreaAtuacao(area);
		vinculo.setDataInicio(dto.dataInicio());

		return toResponseDTO(profissionalAreaRepository.save(vinculo));
	}

	@Transactional(readOnly = true)
	public List<ProfissionalAreaResponseDTO> listar(Long profissionalId) {
		profissionalService.buscarEntidade(profissionalId);
		return profissionalAreaRepository.findByIdProfissionalId(profissionalId).stream()
				.map(this::toResponseDTO)
				.toList();
	}

	/**
	 * Corrige a data de inicio. A area em si nao muda: trocar de area
	 * e desvincular de uma e vincular a outra, porque o par
	 * (profissional, area) e a propria identidade do registro.
	 */
	@Transactional
	public ProfissionalAreaResponseDTO atualizarDataInicio(
			Long profissionalId, Long areaAtuacaoId, ProfissionalAreaRequestDTO dto) {
		ProfissionalArea vinculo = buscarEntidade(profissionalId, areaAtuacaoId);
		vinculo.setDataInicio(dto.dataInicio());
		return toResponseDTO(profissionalAreaRepository.save(vinculo));
	}

	@Transactional
	public void desassociar(Long profissionalId, Long areaAtuacaoId) {
		profissionalAreaRepository.delete(buscarEntidade(profissionalId, areaAtuacaoId));
	}

	private ProfissionalArea buscarEntidade(Long profissionalId, Long areaAtuacaoId) {
		profissionalService.buscarEntidade(profissionalId);
		return profissionalAreaRepository.findById(new ProfissionalAreaId(profissionalId, areaAtuacaoId))
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Profissional não atua nesta área de atuação: área id " + areaAtuacaoId));
	}

	private ProfissionalAreaResponseDTO toResponseDTO(ProfissionalArea vinculo) {
		return new ProfissionalAreaResponseDTO(
				vinculo.getAreaAtuacao().getId(),
				vinculo.getAreaAtuacao().getNome(),
				vinculo.getDataInicio()
		);
	}

}
