package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.ProfissionalAreaRequestDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalAreaResponseDTO;
import br.edu.ifac.astileac.backend.entity.AreaAtuacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.ProfissionalArea;
import br.edu.ifac.astileac.backend.entity.ProfissionalAreaId;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.ProfissionalAreaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Teste de unidade do vinculo profissional x area. O que importa aqui
 * e a chave composta: nao pode haver duas linhas para o mesmo par.
 */
@ExtendWith(MockitoExtension.class)
class ProfissionalAreaServiceTest {

	@Mock
	private ProfissionalAreaRepository profissionalAreaRepository;

	@Mock
	private ProfissionalService profissionalService;

	@Mock
	private AreaAtuacaoService areaAtuacaoService;

	@InjectMocks
	private ProfissionalAreaService profissionalAreaService;

	private Profissional profissional() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		return profissional;
	}

	private AreaAtuacao area(Long id, String nome) {
		AreaAtuacao area = new AreaAtuacao();
		area.setId(id);
		area.setNome(nome);
		return area;
	}

	private ProfissionalArea vinculo(Long areaId, String nome, LocalDate dataInicio) {
		ProfissionalArea vinculo = new ProfissionalArea();
		vinculo.setProfissional(profissional());
		vinculo.setAreaAtuacao(area(areaId, nome));
		vinculo.setDataInicio(dataInicio);
		return vinculo;
	}

	@Test
	void deveAssociarProfissionalAArea() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(areaAtuacaoService.buscarEntidade(2L)).thenReturn(area(2L, "juridica"));
		when(profissionalAreaRepository.existsById(any(ProfissionalAreaId.class))).thenReturn(false);
		when(profissionalAreaRepository.save(any(ProfissionalArea.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ProfissionalAreaResponseDTO resultado = profissionalAreaService.associar(
				1L, new ProfissionalAreaRequestDTO(2L, LocalDate.of(2022, 3, 1)));

		assertEquals(2L, resultado.areaAtuacaoId());
		assertEquals("juridica", resultado.nome());
		assertEquals(LocalDate.of(2022, 3, 1), resultado.dataInicio());
	}

	@Test
	void naoDeveAssociarDuasVezesAMesmaArea() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(areaAtuacaoService.buscarEntidade(2L)).thenReturn(area(2L, "juridica"));
		when(profissionalAreaRepository.existsById(any(ProfissionalAreaId.class))).thenReturn(true);

		assertThrows(ConflitoException.class, () -> profissionalAreaService.associar(
				1L, new ProfissionalAreaRequestDTO(2L, LocalDate.of(2022, 3, 1))));
		verify(profissionalAreaRepository, never()).save(any());
	}

	@Test
	void deveListarTodasAsAreasDoProfissional() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(profissionalAreaRepository.findByIdProfissionalId(1L)).thenReturn(List.of(
				vinculo(1L, "educacional", LocalDate.of(2020, 1, 10)),
				vinculo(2L, "juridica", LocalDate.of(2022, 3, 1))));

		List<ProfissionalAreaResponseDTO> resultado = profissionalAreaService.listar(1L);

		// Um profissional atua em mais de uma area ao mesmo tempo:
		// e o ponto do modelo N:N.
		assertEquals(2, resultado.size());
		assertEquals("educacional", resultado.get(0).nome());
		assertEquals("juridica", resultado.get(1).nome());
	}

	@Test
	void deveCorrigirDataDeInicio() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(profissionalAreaRepository.findById(any(ProfissionalAreaId.class)))
				.thenReturn(Optional.of(vinculo(2L, "juridica", LocalDate.of(2022, 3, 1))));
		when(profissionalAreaRepository.save(any(ProfissionalArea.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ProfissionalAreaResponseDTO resultado = profissionalAreaService.atualizarDataInicio(
				1L, 2L, new ProfissionalAreaRequestDTO(2L, LocalDate.of(2021, 7, 15)));

		assertEquals(LocalDate.of(2021, 7, 15), resultado.dataInicio());
	}

	@Test
	void deveLancarExcecaoAoDesassociarAreaEmQueNaoAtua() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(profissionalAreaRepository.findById(any(ProfissionalAreaId.class))).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class,
				() -> profissionalAreaService.desassociar(1L, 99L));
	}

	@Test
	void deveRepassarErroQuandoAreaNaoExiste() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(areaAtuacaoService.buscarEntidade(99L))
				.thenThrow(new RecursoNaoEncontradoException("Area de atuacao nao encontrada: id 99"));

		assertThrows(RecursoNaoEncontradoException.class, () -> profissionalAreaService.associar(
				1L, new ProfissionalAreaRequestDTO(99L, LocalDate.of(2022, 3, 1))));
	}

}
