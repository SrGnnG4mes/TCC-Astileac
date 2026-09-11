package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.CertificadoRequestDTO;
import br.edu.ifac.astileac.backend.dto.CertificadoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Certificado;
import br.edu.ifac.astileac.backend.entity.Formacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.CertificadoRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
		// O arquivo so chega no upload (Passo 8).
		assertNull(resultado.arquivoPath());
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

}
