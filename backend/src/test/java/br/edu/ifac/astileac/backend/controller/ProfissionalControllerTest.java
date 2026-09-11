package br.edu.ifac.astileac.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.ifac.astileac.backend.dto.ProfissionalResponseDTO;
import br.edu.ifac.astileac.backend.exception.CpfJaCadastradoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.service.ProfissionalService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de fatia web: so a camada HTTP (controller + GlobalExceptionHandler),
 * com o ProfissionalService mockado. addFilters = false porque a seguranca
 * real ainda nao existe (RF09, Passo 6 -- ver SecurityConfig).
 */
@WebMvcTest(ProfissionalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfissionalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProfissionalService profissionalService;

	private ProfissionalResponseDTO respostaValida() {
		return new ProfissionalResponseDTO(
				1L, "Maria Silva", "12345678901", LocalDate.of(1990, 5, 10), LocalDate.of(2024, 1, 15), true);
	}

	@Test
	void deveCriarProfissionalERetornar201() throws Exception {
		when(profissionalService.criar(any())).thenReturn(respostaValida());

		mockMvc.perform(post("/profissionais")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Maria Silva","cpf":"12345678901","dataNascimento":"1990-05-10","dataAssociacao":"2024-01-15"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nome").value("Maria Silva"));
	}

	@Test
	void deveRetornar400QuandoNomeEmBrancoECpfInvalido() throws Exception {
		mockMvc.perform(post("/profissionais")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"","cpf":"123","dataNascimento":"1990-05-10","dataAssociacao":"2024-01-15"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveRetornar409QuandoCpfJaCadastrado() throws Exception {
		when(profissionalService.criar(any())).thenThrow(new CpfJaCadastradoException("12345678901"));

		mockMvc.perform(post("/profissionais")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Maria Silva","cpf":"12345678901","dataNascimento":"1990-05-10","dataAssociacao":"2024-01-15"}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void deveRetornar404QuandoProfissionalNaoEncontrado() throws Exception {
		when(profissionalService.buscarPorId(999L))
				.thenThrow(new RecursoNaoEncontradoException("Profissional nao encontrado: id 999"));

		mockMvc.perform(get("/profissionais/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deveListarProfissionais() throws Exception {
		when(profissionalService.listar()).thenReturn(List.of(respostaValida()));

		mockMvc.perform(get("/profissionais"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nome").value("Maria Silva"));
	}

	@Test
	void deveRemoverERetornar204() throws Exception {
		mockMvc.perform(delete("/profissionais/1"))
				.andExpect(status().isNoContent());

		verify(profissionalService).remover(anyLong());
	}

}
