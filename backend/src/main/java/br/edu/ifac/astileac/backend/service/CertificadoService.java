package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.CertificadoRequestDTO;
import br.edu.ifac.astileac.backend.dto.CertificadoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Certificado;
import br.edu.ifac.astileac.backend.entity.Formacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.CertificadoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Certificados do profissional (RF06). Relacao 1:N, com ligacao
 * OPCIONAL a uma formacao.
 *
 * O caminho do arquivo digitalizado (arquivo_path) nao e tocado aqui:
 * fica nulo ate o upload do Passo 8. A V5 tornou a coluna opcional
 * justamente porque o certificado e registrado antes de o documento
 * ser anexado.
 */
@Service
@RequiredArgsConstructor
public class CertificadoService {

	private final CertificadoRepository certificadoRepository;
	private final ProfissionalService profissionalService;
	private final FormacaoService formacaoService;

	@Transactional
	public CertificadoResponseDTO criar(Long profissionalId, CertificadoRequestDTO dto) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);

		Certificado certificado = new Certificado();
		certificado.setProfissional(profissional);
		aplicar(certificado, profissionalId, dto);

		return toResponseDTO(certificadoRepository.save(certificado));
	}

	@Transactional(readOnly = true)
	public List<CertificadoResponseDTO> listar(Long profissionalId) {
		profissionalService.buscarEntidade(profissionalId);
		return certificadoRepository.findByProfissionalId(profissionalId).stream()
				.map(this::toResponseDTO)
				.toList();
	}

	@Transactional
	public CertificadoResponseDTO atualizar(Long profissionalId, Long certificadoId, CertificadoRequestDTO dto) {
		profissionalService.buscarEntidade(profissionalId);

		Certificado certificado = buscarEntidade(profissionalId, certificadoId);
		aplicar(certificado, profissionalId, dto);

		return toResponseDTO(certificadoRepository.save(certificado));
	}

	@Transactional
	public void remover(Long profissionalId, Long certificadoId) {
		profissionalService.buscarEntidade(profissionalId);
		certificadoRepository.delete(buscarEntidade(profissionalId, certificadoId));
	}

	private void aplicar(Certificado certificado, Long profissionalId, CertificadoRequestDTO dto) {
		certificado.setFormacao(resolverFormacao(profissionalId, dto.formacaoId()));
		certificado.setTitulo(dto.titulo().trim());
		certificado.setInstituicaoEmissora(dto.instituicaoEmissora().trim());
		certificado.setCargaHoraria(dto.cargaHoraria());
		certificado.setDataEmissao(dto.dataEmissao());
	}

	/**
	 * A formacao vinculada tem que ser DO MESMO profissional.
	 *
	 * Sem essa checagem, bastaria enviar o id da formacao de outra
	 * pessoa para pendurar um certificado nela -- e, como formacao e
	 * certificado sao o que comprova habilitacao legal (Lei
	 * 14.704/2023), isso corromperia justamente o dado que o sistema
	 * existe para tornar confiavel. O buscarEntidade do FormacaoService
	 * ja faz esse filtro.
	 */
	private Formacao resolverFormacao(Long profissionalId, Long formacaoId) {
		if (formacaoId == null) {
			return null;
		}
		return formacaoService.buscarEntidade(profissionalId, formacaoId);
	}

	private Certificado buscarEntidade(Long profissionalId, Long certificadoId) {
		return certificadoRepository.findById(certificadoId)
				.filter(certificado -> certificado.getProfissional().getId().equals(profissionalId))
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Certificado nao encontrado para este profissional: id " + certificadoId));
	}

	private CertificadoResponseDTO toResponseDTO(Certificado certificado) {
		return new CertificadoResponseDTO(
				certificado.getId(),
				certificado.getFormacao() != null ? certificado.getFormacao().getId() : null,
				certificado.getTitulo(),
				certificado.getInstituicaoEmissora(),
				certificado.getCargaHoraria(),
				certificado.getDataEmissao(),
				certificado.getArquivoPath()
		);
	}

}
