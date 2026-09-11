package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.AreaAtuacaoRequestDTO;
import br.edu.ifac.astileac.backend.dto.AreaAtuacaoResponseDTO;
import br.edu.ifac.astileac.backend.entity.AreaAtuacao;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.AreaAtuacaoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AreaAtuacaoServiceTest {

	@Mock
	private AreaAtuacaoRepository areaAtuacaoRepository;

	@InjectMocks
	private AreaAtuacaoService areaAtuacaoService;

	@Test
	void deveNormalizarONomeParaMinusculas() {
		when(areaAtuacaoRepository.existsByNome("interpretacao remota")).thenReturn(false);
		when(areaAtuacaoRepository.save(any(AreaAtuacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AreaAtuacaoResponseDTO resultado = areaAtuacaoService.criar(
				new AreaAtuacaoRequestDTO("  Interpretacao Remota  ", "Atendimento a distancia"));

		// Guardar normalizado evita que "Juridica" e "juridica"
		// convivam como areas distintas.
		assertEquals("interpretacao remota", resultado.nome());
	}

	@Test
	void naoDeveCriarAreaComNomeRepetidoMesmoComOutraCaixa() {
		when(areaAtuacaoRepository.existsByNome("juridica")).thenReturn(true);

		assertThrows(ConflitoException.class,
				() -> areaAtuacaoService.criar(new AreaAtuacaoRequestDTO("JURIDICA", null)));
		verify(areaAtuacaoRepository, never()).save(any());
	}

	@Test
	void deveLancarExcecaoAoBuscarAreaInexistente() {
		when(areaAtuacaoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> areaAtuacaoService.buscarPorId(99L));
	}

}
