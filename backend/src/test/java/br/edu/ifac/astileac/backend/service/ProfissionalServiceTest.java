package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.ProfissionalRequestDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResponseDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResumoDTO;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.CpfJaCadastradoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Teste de unidade: so a regra de negocio do ProfissionalService,
 * com o repository mockado (sem banco, sem contexto Spring).
 *
 * Os CPFs usados aqui sao validos de verdade (digitos verificadores
 * conferem), para que os mesmos dados sirvam nos testes de controller,
 * onde a validacao roda.
 */
@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {

	private static final String CPF_VALIDO = "11144477735";
	private static final String OUTRO_CPF_VALIDO = "52998224725";

	@Mock
	private ProfissionalRepository profissionalRepository;

	@InjectMocks
	private ProfissionalService profissionalService;

	private ProfissionalRequestDTO dtoValido() {
		return new ProfissionalRequestDTO(
				"Maria Silva", CPF_VALIDO, LocalDate.of(1990, 5, 10), LocalDate.of(2024, 1, 15));
	}

	private Profissional entidadeExistente() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		profissional.setNome("Maria Silva");
		profissional.setCpf(CPF_VALIDO);
		profissional.setDataNascimento(LocalDate.of(1990, 5, 10));
		profissional.setDataAssociacao(LocalDate.of(2024, 1, 15));
		profissional.setAtivo(true);
		return profissional;
	}

	@Test
	void deveCriarProfissionalQuandoCpfNaoExiste() {
		when(profissionalRepository.existsByCpf(CPF_VALIDO)).thenReturn(false);
		when(profissionalRepository.save(any(Profissional.class))).thenAnswer(invocation -> {
			Profissional salvo = invocation.getArgument(0);
			salvo.setId(1L);
			return salvo;
		});

		ProfissionalResponseDTO resultado = profissionalService.criar(dtoValido());

		assertEquals(1L, resultado.id());
		assertEquals("Maria Silva", resultado.nome());
		assertTrue(resultado.ativo());
	}

	@Test
	void deveLancarExcecaoAoCriarComCpfJaCadastrado() {
		when(profissionalRepository.existsByCpf(CPF_VALIDO)).thenReturn(true);

		assertThrows(CpfJaCadastradoException.class, () -> profissionalService.criar(dtoValido()));
		verify(profissionalRepository, never()).save(any());
	}

	@Test
	void mensagemDeCpfDuplicadoNaoDeveConterOCpf() {
		when(profissionalRepository.existsByCpf(CPF_VALIDO)).thenReturn(true);

		CpfJaCadastradoException excecao = assertThrows(
				CpfJaCadastradoException.class, () -> profissionalService.criar(dtoValido()));

		// A mensagem vai para o cliente da API: repetir o CPF
		// permitiria descobrir quem e associado testando numeros.
		assertTrue(!excecao.getMessage().contains(CPF_VALIDO));
	}

	@Test
	void deveRetornarProfissionalAoBuscarIdExistente() {
		when(profissionalRepository.findById(1L)).thenReturn(Optional.of(entidadeExistente()));

		ProfissionalResponseDTO resultado = profissionalService.buscarPorId(1L);

		assertEquals("Maria Silva", resultado.nome());
	}

	@Test
	void deveLancarExcecaoAoBuscarIdInexistente() {
		when(profissionalRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> profissionalService.buscarPorId(999L));
	}

	@Test
	void deveListarProfissionaisPaginadosComCpfMascarado() {
		Pageable pagina = PageRequest.of(0, 20);
		when(profissionalRepository.findByDataExclusaoSolicitadaIsNull(pagina))
				.thenReturn(new PageImpl<>(List.of(entidadeExistente()), pagina, 1));

		Page<ProfissionalResumoDTO> resultado = profissionalService.listar(pagina);

		assertEquals(1, resultado.getTotalElements());
		assertEquals("Maria Silva", resultado.getContent().get(0).nome());
		// 11144477735 -> mostra apenas os cinco ultimos digitos.
		assertEquals("***.***.777-35", resultado.getContent().get(0).cpfMascarado());
	}

	@Test
	void deveAtualizarProfissionalSemAlterarCpf() {
		when(profissionalRepository.findById(1L)).thenReturn(Optional.of(entidadeExistente()));
		when(profissionalRepository.save(any(Profissional.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProfissionalRequestDTO dto = new ProfissionalRequestDTO(
				"Maria Silva Santos", CPF_VALIDO, LocalDate.of(1990, 5, 10), LocalDate.of(2024, 1, 15));
		ProfissionalResponseDTO resultado = profissionalService.atualizar(1L, dto);

		assertEquals("Maria Silva Santos", resultado.nome());
		// CPF nao mudou: nao precisa checar duplicidade.
		verify(profissionalRepository, never()).existsByCpf(any());
	}

	@Test
	void deveLancarExcecaoAoAtualizarParaCpfDeOutroProfissional() {
		when(profissionalRepository.findById(1L)).thenReturn(Optional.of(entidadeExistente()));
		when(profissionalRepository.existsByCpf(OUTRO_CPF_VALIDO)).thenReturn(true);

		ProfissionalRequestDTO dto = new ProfissionalRequestDTO(
				"Maria Silva", OUTRO_CPF_VALIDO, LocalDate.of(1990, 5, 10), LocalDate.of(2024, 1, 15));

		assertThrows(CpfJaCadastradoException.class, () -> profissionalService.atualizar(1L, dto));
	}

	@Test
	void deveRemoverProfissionalExistente() {
		Profissional existente = entidadeExistente();
		when(profissionalRepository.findById(1L)).thenReturn(Optional.of(existente));

		profissionalService.remover(1L);

		verify(profissionalRepository).delete(existente);
	}

	@Test
	void deveLancarExcecaoAoRemoverIdInexistente() {
		when(profissionalRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> profissionalService.remover(999L));
	}

}
