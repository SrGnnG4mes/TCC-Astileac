package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.ContatoRequestDTO;
import br.edu.ifac.astileac.backend.dto.ContatoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Contato;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.TipoContato;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.ContatoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Teste de unidade do ContatoService. Foco nas duas regras que nao sao
 * CRUD: o formato do valor depende do tipo, e so um contato pode ser
 * principal por tipo.
 */
@ExtendWith(MockitoExtension.class)
class ContatoServiceTest {

	@Mock
	private ContatoRepository contatoRepository;

	@Mock
	private ProfissionalService profissionalService;

	@InjectMocks
	private ContatoService contatoService;

	private Profissional profissional() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		return profissional;
	}

	private Contato contatoExistente(Long id, TipoContato tipo, boolean principal) {
		Contato contato = new Contato();
		contato.setId(id);
		contato.setProfissional(profissional());
		contato.setTipo(tipo);
		contato.setValor(tipo == TipoContato.EMAIL ? "maria@exemplo.com" : "68999998888");
		contato.setPrincipal(principal);
		return contato;
	}

	@Test
	void deveCriarContatoDeEmailValido() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(contatoRepository.save(any(Contato.class))).thenAnswer(invocation -> {
			Contato salvo = invocation.getArgument(0);
			salvo.setId(10L);
			return salvo;
		});

		ContatoResponseDTO resultado = contatoService.criar(
				1L, new ContatoRequestDTO(TipoContato.EMAIL, "maria@exemplo.com", false));

		assertEquals(10L, resultado.id());
		assertEquals("maria@exemplo.com", resultado.valor());
		assertFalse(resultado.principal());
	}

	@Test
	void deveRecusarEmailQueNaoParaceEmail() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());

		assertThrows(RegraDeNegocioException.class, () -> contatoService.criar(
				1L, new ContatoRequestDTO(TipoContato.EMAIL, "maria.exemplo.com", false)));
		verify(contatoRepository, never()).save(any());
	}

	@Test
	void deveRecusarTelefoneComPontuacao() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());

		assertThrows(RegraDeNegocioException.class, () -> contatoService.criar(
				1L, new ContatoRequestDTO(TipoContato.TELEFONE, "(68) 99999-8888", false)));
	}

	@Test
	void deveAceitarTelefoneDeCelularComOnzeDigitos() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(contatoRepository.save(any(Contato.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ContatoResponseDTO resultado = contatoService.criar(
				1L, new ContatoRequestDTO(TipoContato.TELEFONE, "68999998888", false));

		assertEquals("68999998888", resultado.valor());
	}

	@Test
	void aoMarcarNovoPrincipalDeveDesmarcarOAnteriorDoMesmoTipo() {
		Contato anterior = contatoExistente(5L, TipoContato.EMAIL, true);
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(contatoRepository.findByProfissionalIdAndTipoAndPrincipalTrue(1L, TipoContato.EMAIL))
				.thenReturn(List.of(anterior));
		when(contatoRepository.save(any(Contato.class))).thenAnswer(invocation -> invocation.getArgument(0));

		contatoService.criar(1L, new ContatoRequestDTO(TipoContato.EMAIL, "novo@exemplo.com", true));

		assertFalse(anterior.getPrincipal(), "o e-mail principal anterior deveria ter sido desmarcado");
	}

	@Test
	void naoDeveMexerNoPrincipalDeOutroTipo() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(contatoRepository.findByProfissionalIdAndTipoAndPrincipalTrue(1L, TipoContato.TELEFONE))
				.thenReturn(List.of());
		when(contatoRepository.save(any(Contato.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ContatoResponseDTO resultado = contatoService.criar(
				1L, new ContatoRequestDTO(TipoContato.TELEFONE, "68999998888", true));

		// O e-mail principal do profissional nao foi consultado nem alterado.
		verify(contatoRepository, never())
				.findByProfissionalIdAndTipoAndPrincipalTrue(1L, TipoContato.EMAIL);
		assertTrue(resultado.principal());
	}

	@Test
	void naoDeveAlterarContatoDeOutroProfissional() {
		Contato deOutroDono = contatoExistente(7L, TipoContato.EMAIL, false);
		Profissional outro = new Profissional();
		outro.setId(2L);
		deOutroDono.setProfissional(outro);

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(contatoRepository.findById(7L)).thenReturn(Optional.of(deOutroDono));

		assertThrows(RecursoNaoEncontradoException.class, () -> contatoService.atualizar(
				1L, 7L, new ContatoRequestDTO(TipoContato.EMAIL, "invasor@exemplo.com", false)));
	}

	@Test
	void deveListarContatosDoProfissional() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(contatoRepository.findByProfissionalId(1L))
				.thenReturn(List.of(contatoExistente(1L, TipoContato.EMAIL, true),
						contatoExistente(2L, TipoContato.TELEFONE, false)));

		List<ContatoResponseDTO> resultado = contatoService.listar(1L);

		assertEquals(2, resultado.size());
	}

}
