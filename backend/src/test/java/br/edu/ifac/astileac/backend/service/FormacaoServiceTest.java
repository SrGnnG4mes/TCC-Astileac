package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.FormacaoRequestDTO;
import br.edu.ifac.astileac.backend.dto.FormacaoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Formacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.FormacaoRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Teste de unidade do FormacaoService. O foco sao as duas checagens
 * que o Bean Validation nao alcanca, porque dependem da data de hoje
 * ou de um dado do profissional.
 */
@ExtendWith(MockitoExtension.class)
class FormacaoServiceTest {

	@Mock
	private FormacaoRepository formacaoRepository;

	@Mock
	private ProfissionalService profissionalService;

	@InjectMocks
	private FormacaoService formacaoService;

	private Profissional profissional() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		profissional.setDataNascimento(LocalDate.of(1990, 5, 10));
		return profissional;
	}

	private FormacaoRequestDTO dto(int anoConclusao) {
		return new FormacaoRequestDTO(
				"graduacao", "Letras Libras", "Universidade Federal do Acre", anoConclusao);
	}

	@Test
	void deveCriarFormacaoValida() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(formacaoRepository.save(any(Formacao.class))).thenAnswer(invocation -> {
			Formacao salva = invocation.getArgument(0);
			salva.setId(20L);
			return salva;
		});

		FormacaoResponseDTO resultado = formacaoService.criar(1L, dto(2015));

		assertEquals(20L, resultado.id());
		assertEquals("Letras Libras", resultado.curso());
		assertEquals(2015, resultado.anoConclusao());
	}

	@Test
	void naoDeveAceitarFormacaoConcluidaNoFuturo() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());

		int anoQueVem = LocalDate.now().getYear() + 1;

		assertThrows(RegraDeNegocioException.class, () -> formacaoService.criar(1L, dto(anoQueVem)));
		verify(formacaoRepository, never()).save(any());
	}

	@Test
	void naoDeveAceitarFormacaoAnteriorAoNascimento() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());

		// Profissional nasceu em 1990.
		assertThrows(RegraDeNegocioException.class, () -> formacaoService.criar(1L, dto(1985)));
		verify(formacaoRepository, never()).save(any());
	}

	@Test
	void naoDeveAlcancarFormacaoDeOutroProfissional() {
		Profissional outro = new Profissional();
		outro.setId(2L);
		Formacao deOutroDono = new Formacao();
		deOutroDono.setId(9L);
		deOutroDono.setProfissional(outro);

		when(formacaoRepository.findById(9L)).thenReturn(Optional.of(deOutroDono));

		assertThrows(RecursoNaoEncontradoException.class,
				() -> formacaoService.buscarEntidade(1L, 9L));
	}

}
