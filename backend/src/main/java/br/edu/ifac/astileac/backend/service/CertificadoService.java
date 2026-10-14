package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.CertificadoRequestDTO;
import br.edu.ifac.astileac.backend.dto.CertificadoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Certificado;
import br.edu.ifac.astileac.backend.entity.Formacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.CertificadoRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
	private final ArquivoCertificadoService arquivoCertificadoService;

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
		Certificado certificado = buscarEntidade(profissionalId, certificadoId);
		certificadoRepository.delete(certificado);

		// O documento sai junto com o registro. Sem isto ficava orfao no
		// disco: dado pessoal sem nenhum registro que apontasse para ele,
		// invisivel ate para a diretoria. Achado pelo teste de integracao.
		arquivoCertificadoService.apagarAoConfirmar(certificado.getArquivoPath());
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
						"Certificado não encontrado para este profissional: id " + certificadoId));
	}

	// ---------- documento e reconhecimento (Passo 8 / RF13) ----------

	/**
	 * Anexa ou substitui o documento digitalizado.
	 *
	 * SUBSTITUIR O ARQUIVO DERRUBA A VALIDACAO. Sem isso abre-se um
	 * buraco simples: anexa-se o documento A, espera-se o reconhecimento
	 * da diretoria, e troca-se pelo documento B -- que ficaria validado
	 * sem nunca ter sido visto. Como o sistema existe para tornar o dado
	 * de habilitacao confiavel, e justamente aqui que o selo precisa
	 * significar alguma coisa.
	 */
	@Transactional
	public CertificadoResponseDTO anexarDocumento(
			Long profissionalId, Long certificadoId, MultipartFile arquivo) {

		profissionalService.buscarEntidade(profissionalId);
		Certificado certificado = buscarEntidade(profissionalId, certificadoId);

		String anterior = certificado.getArquivoPath();
		certificado.setArquivoPath(arquivoCertificadoService.guardar(arquivo));
		certificado.setValidadoEm(null);
		certificado.setValidadoPor(null);

		Certificado salvo = certificadoRepository.save(certificado);

		// So depois de a troca estar confirmada no banco: se apagassemos
		// antes e a transacao falhasse, o certificado ficaria apontando
		// para um arquivo que nao existe mais.
		arquivoCertificadoService.apagarAoConfirmar(anterior);

		return toResponseDTO(salvo);
	}

	/** Caminho em disco para o download, ja conferido. */
	@Transactional(readOnly = true)
	public Path caminhoDoDocumento(Long profissionalId, Long certificadoId) {
		profissionalService.buscarEntidade(profissionalId);
		Certificado certificado = buscarEntidade(profissionalId, certificadoId);

		if (!certificado.temDocumento()) {
			throw new RecursoNaoEncontradoException("Este certificado não tem documento anexado.");
		}

		return arquivoCertificadoService.resolver(certificado.getArquivoPath());
	}

	/**
	 * Reconhecimento do documento pela diretoria (RF13).
	 *
	 * Fica no proprio registro, e nao apenas no log de acesso, porque e
	 * propriedade duravel do certificado: quem consulta a ficha precisa
	 * saber se aquele documento ja foi visto, sem ter de cruzar
	 * auditoria.
	 */
	@Transactional
	public CertificadoResponseDTO validar(Long profissionalId, Long certificadoId, String login) {
		profissionalService.buscarEntidade(profissionalId);
		Certificado certificado = buscarEntidade(profissionalId, certificadoId);

		if (!certificado.temDocumento()) {
			throw new RegraDeNegocioException(
					"Não há documento anexado para dar o visto. "
							+ "Peça ao associado que envie o certificado em PDF.");
		}

		if (certificado.estaValidado()) {
			throw new ConflitoException("Este documento já recebeu o visto em "
					+ certificado.getValidadoEm().toLocalDate());
		}

		certificado.setValidadoEm(LocalDateTime.now());
		certificado.setValidadoPor(login);

		return toResponseDTO(certificadoRepository.save(certificado));
	}

	/** Desfaz o reconhecimento -- para o caso de ter sido feito por engano. */
	@Transactional
	public CertificadoResponseDTO invalidar(Long profissionalId, Long certificadoId) {
		profissionalService.buscarEntidade(profissionalId);
		Certificado certificado = buscarEntidade(profissionalId, certificadoId);

		certificado.setValidadoEm(null);
		certificado.setValidadoPor(null);

		return toResponseDTO(certificadoRepository.save(certificado));
	}

	private CertificadoResponseDTO toResponseDTO(Certificado certificado) {
		return new CertificadoResponseDTO(
				certificado.getId(),
				certificado.getFormacao() != null ? certificado.getFormacao().getId() : null,
				certificado.getTitulo(),
				certificado.getInstituicaoEmissora(),
				certificado.getCargaHoraria(),
				certificado.getDataEmissao(),
				certificado.temDocumento(),
				certificado.getValidadoEm(),
				certificado.getValidadoPor()
		);
	}

}
