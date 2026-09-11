package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.VinculoEmpregaticioRequestDTO;
import br.edu.ifac.astileac.backend.dto.VinculoEmpregaticioResponseDTO;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.VinculoEmpregaticio;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.VinculoEmpregaticioRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Teste de unidade do VinculoEmpregaticioService. O comportamento
 * central: "atual" e derivado de data_fim, nunca recebido do cliente.
 */
@ExtendWith(MockitoExtension.class)
class VinculoEmpregaticioServiceTest {

	@Mock
	private VinculoEmpregaticioRepository vinculoEmpregaticioRepository;

	@Mock
	private ProfissionalService profissionalService;

	@InjectMocks
	private VinculoEmpregaticioService vinculoEmpregaticioService;

	private Profissional profissional() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		return profissional;
	}

	private VinculoEmpregaticioRequestDTO dto(LocalDate dataInicio, LocalDate dataFim) {
		return new VinculoEmpregaticioRequestDTO(
				"Instituto Federal do Acre", "Interprete de Libras", "efetivo", dataInicio, dataFim);
	}

	@Test
	void vinculoSemDataFimDeveSerMarcadoComoAtual() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(vinculoEmpregaticioRepository.save(any(VinculoEmpregaticio.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		VinculoEmpregaticioResponseDTO resultado = vinculoEmpregaticioService.criar(
				1L, dto(LocalDate.of(2020, 3, 1), null));

		assertTrue(resultado.atual(), "data_fim nula significa vinculo em andamento");
	}

	@Test
	void vinculoComDataFimNaoDeveSerAtual() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(vinculoEmpregaticioRepository.save(any(VinculoEmpregaticio.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		VinculoEmpregaticioResponseDTO resultado = vinculoEmpregaticioService.criar(
				1L, dto(LocalDate.of(2018, 2, 1), LocalDate.of(2021, 12, 31)));

		// Derivado: o cliente nao tem como enviar uma contradicao.
		assertFalse(resultado.atual());
	}

	@Test
	void naoDeveAceitarDataFimAnteriorAoInicio() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());

		assertThrows(RegraDeNegocioException.class, () -> vinculoEmpregaticioService.criar(
				1L, dto(LocalDate.of(2020, 3, 1), LocalDate.of(2019, 1, 1))));
		verify(vinculoEmpregaticioRepository, never()).save(any());
	}

	@Test
	void deveListarOHistoricoInteiroEncerradosEEmAndamento() {
		VinculoEmpregaticio encerrado = new VinculoEmpregaticio();
		encerrado.setId(1L);
		encerrado.setProfissional(profissional());
		encerrado.setDataInicio(LocalDate.of(2015, 1, 1));
		encerrado.setDataFim(LocalDate.of(2019, 6, 30));
		encerrado.setAtual(false);

		VinculoEmpregaticio emAndamento = new VinculoEmpregaticio();
		emAndamento.setId(2L);
		emAndamento.setProfissional(profissional());
		emAndamento.setDataInicio(LocalDate.of(2020, 3, 1));
		emAndamento.setAtual(true);

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(vinculoEmpregaticioRepository.findByProfissionalId(1L))
				.thenReturn(List.of(encerrado, emAndamento));

		List<VinculoEmpregaticioResponseDTO> resultado = vinculoEmpregaticioService.listar(1L);

		// O historico e mantido, nao so o vinculo vigente.
		assertEquals(2, resultado.size());
		assertFalse(resultado.get(0).atual());
		assertTrue(resultado.get(1).atual());
	}

}
