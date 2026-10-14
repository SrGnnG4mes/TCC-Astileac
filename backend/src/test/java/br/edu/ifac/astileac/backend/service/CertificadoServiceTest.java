package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.CertificadoRequestDTO;
import br.edu.ifac.astileac.backend.dto.CertificadoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Certificado;
import br.edu.ifac.astileac.backend.entity.Formacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.CertificadoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Teste de unidade do CertificadoService. A regra que mais importa:
 * a formacao vinculada tem que ser do MESMO profissional.
 */
@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {

	@Mock
	private CertificadoRepository certificadoRepository;

	@Mock
	private ProfissionalService profissionalService;

	@Mock
	private FormacaoService formacaoService;

	@Mock
	private ArquivoCertificadoService arquivoCertificadoService;

	@InjectMocks
	private CertificadoService certificadoService;

	private Profissional profissional() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		return profissional;
	}

	private CertificadoRequestDTO dto(Long formacaoId) {
		return new CertificadoRequestDTO(
				formacaoId, "Interpretacao em contexto juridico", "ASTILEAC",
				40, LocalDate.of(2023, 6, 15));
	}

	@Test
	void deveCriarCertificadoSemFormacaoVinculada() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(certificadoRepository.save(any(Certificado.class))).thenAnswer(invocation -> {
			Certificado salvo = invocation.getArgument(0);
			salvo.setId(30L);
			return salvo;
		});

		CertificadoResponseDTO resultado = certificadoService.criar(1L, dto(null));

		// Curso livre: nao comprova uma formacao registrada.
		assertNull(resultado.formacaoId());
		assertEquals(40, resultado.cargaHoraria());
		// O documento so chega no upload, e o reconhecimento depois dele.
		assertFalse(resultado.temDocumento());
		assertNull(resultado.validadoEm());
	}

	@Test
	void deveVincularCertificadoAFormacaoDoMesmoProfissional() {
		Formacao formacao = new Formacao();
		formacao.setId(20L);
		formacao.setProfissional(profissional());

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(formacaoService.buscarEntidade(1L, 20L)).thenReturn(formacao);
		when(certificadoRepository.save(any(Certificado.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CertificadoResponseDTO resultado = certificadoService.criar(1L, dto(20L));

		assertEquals(20L, resultado.formacaoId());
	}

	@Test
	void naoDeveVincularCertificadoAFormacaoDeOutroProfissional() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		// O FormacaoService filtra por dono e nao encontra.
		when(formacaoService.buscarEntidade(1L, 99L))
				.thenThrow(new RecursoNaoEncontradoException("Formacao nao encontrada para este profissional: id 99"));

		assertThrows(RecursoNaoEncontradoException.class, () -> certificadoService.criar(1L, dto(99L)));
		verify(certificadoRepository, never()).save(any());
	}

	// ---------- documento e reconhecimento (Passo 8 / RF13) ----------

	/**
	 * A regra central do RF13.
	 *
	 * Sem ela abre-se um buraco simples: anexa-se o documento A, espera-se
	 * o reconhecimento da diretoria, e troca-se pelo documento B, que
	 * ficaria validado sem nunca ter sido visto. O selo so significa algo
	 * se cair quando o documento muda.
	 */
	@Test
	void anexarDocumentoNovoDeveDerrubarOReconhecimentoAnterior() {
		Certificado validado = certificadoExistente();
		validado.setArquivoPath("certificados/antigo.pdf");
		validado.setValidadoEm(LocalDateTime.of(2026, 1, 10, 9, 0));
		validado.setValidadoPor("diretora");

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(certificadoRepository.findById(30L)).thenReturn(Optional.of(validado));
		when(arquivoCertificadoService.guardar(any())).thenReturn("certificados/novo.pdf");
		when(certificadoRepository.save(any(Certificado.class))).thenAnswer(c -> c.getArgument(0));

		CertificadoResponseDTO resultado = certificadoService.anexarDocumento(1L, 30L, arquivoFalso());

		assertTrue(resultado.temDocumento());
		assertNull(resultado.validadoEm(), "substituir o documento tem de derrubar o reconhecimento");
		assertNull(resultado.validadoPor());

		// O arquivo anterior e removido, e so depois de a troca estar
		// gravada -- apagar antes deixaria o registro apontando para um
		// arquivo inexistente se a gravacao falhasse.
		verify(arquivoCertificadoService).apagarAoConfirmar("certificados/antigo.pdf");
	}

	@Test
	void naoDeveReconhecerCertificadoSemDocumento() {
		Certificado semArquivo = certificadoExistente();

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(certificadoRepository.findById(30L)).thenReturn(Optional.of(semArquivo));

		assertThrows(RegraDeNegocioException.class,
				() -> certificadoService.validar(1L, 30L, "diretora"));

		verify(certificadoRepository, never()).save(any());
	}

	@Test
	void deveRegistrarQuemReconheceuEQuando() {
		Certificado comArquivo = certificadoExistente();
		comArquivo.setArquivoPath("certificados/doc.pdf");

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(certificadoRepository.findById(30L)).thenReturn(Optional.of(comArquivo));
		when(certificadoRepository.save(any(Certificado.class))).thenAnswer(c -> c.getArgument(0));

		CertificadoResponseDTO resultado = certificadoService.validar(1L, 30L, "diretora");

		assertEquals("diretora", resultado.validadoPor());
		assertNotNull(resultado.validadoEm());
	}

	@Test
	void naoDeveReconhecerDuasVezes() {
		Certificado jaValidado = certificadoExistente();
		jaValidado.setArquivoPath("certificados/doc.pdf");
		jaValidado.setValidadoEm(LocalDateTime.of(2026, 1, 10, 9, 0));
		jaValidado.setValidadoPor("diretora");

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(certificadoRepository.findById(30L)).thenReturn(Optional.of(jaValidado));

		assertThrows(ConflitoException.class,
				() -> certificadoService.validar(1L, 30L, "outra-diretora"));
	}

	@Test
	void naoDeveBaixarDocumentoDeCertificadoSemArquivo() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(certificadoRepository.findById(30L)).thenReturn(Optional.of(certificadoExistente()));

		assertThrows(RecursoNaoEncontradoException.class,
				() -> certificadoService.caminhoDoDocumento(1L, 30L));
	}

	private Certificado certificadoExistente() {
		Certificado certificado = new Certificado();
		certificado.setId(30L);
		certificado.setProfissional(profissional());
		certificado.setTitulo("Interpretacao em contexto juridico");
		certificado.setInstituicaoEmissora("ASTILEAC");
		certificado.setCargaHoraria(40);
		certificado.setDataEmissao(LocalDate.of(2023, 6, 15));
		return certificado;
	}

	private MultipartFile arquivoFalso() {
		return new MockMultipartFile("arquivo", "x.pdf", "application/pdf", "%PDF-1.4".getBytes());
	}

}
