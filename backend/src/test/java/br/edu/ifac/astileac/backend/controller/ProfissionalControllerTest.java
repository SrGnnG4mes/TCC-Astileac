package br.edu.ifac.astileac.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.ifac.astileac.backend.dto.ProfissionalResponseDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalResumoDTO;
import br.edu.ifac.astileac.backend.exception.CpfJaCadastradoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.service.LogAcessoService;
import br.edu.ifac.astileac.backend.service.ProfissionalService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

	private static final String CPF_VALIDO = "11144477735";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProfissionalService profissionalService;

	/**
	 * O @WebMvcTest carrega os WebMvcConfigurer -- logo o WebConfig e o
	 * LogAcessoInterceptor --, mas nao carrega @Service. Sem este mock o
	 * contexto nem sobe. A auditoria (RF11) tem teste proprio.
	 */
	@MockitoBean
	private LogAcessoService logAcessoService;

	private ProfissionalResponseDTO respostaValida() {
		return new ProfissionalResponseDTO(
				1L, "Maria Silva", CPF_VALIDO, LocalDate.of(1990, 5, 10), LocalDate.of(2024, 1, 15), true, null);
	}

	private String corpoValido() {
		return """
				{"nome":"Maria Silva","cpf":"%s","dataNascimento":"1990-05-10","dataAssociacao":"2024-01-15"}
				""".formatted(CPF_VALIDO);
	}

	@Test
	void deveCriarProfissionalERetornar201() throws Exception {
		when(profissionalService.criar(any())).thenReturn(respostaValida());

		mockMvc.perform(post("/profissionais")
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpoValido()))
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
	void deveRetornar400QuandoCpfTemFormatoCertoMasDigitoVerificadorErrado() throws Exception {
		// 11 digitos, mas nao e um CPF real.
		mockMvc.perform(post("/profissionais")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Maria Silva","cpf":"12345678901","dataNascimento":"1990-05-10","dataAssociacao":"2024-01-15"}
								"""))
				.andExpect(status().isBadRequest());

		verify(profissionalService, never()).criar(any());
	}

	@Test
	void deveRetornar400QuandoCorpoEMalformado() throws Exception {
		mockMvc.perform(post("/profissionais")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": "))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveRetornar400QuandoIdNaoENumero() throws Exception {
		mockMvc.perform(get("/profissionais/abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveRetornar409QuandoCpfJaCadastrado() throws Exception {
		when(profissionalService.criar(any())).thenThrow(new CpfJaCadastradoException());

		mockMvc.perform(post("/profissionais")
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpoValido()))
				.andExpect(status().isConflict())
				// A resposta nao pode devolver o CPF consultado.
				.andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString(CPF_VALIDO))));
	}

	@Test
	void deveRetornar409AoRemoverProfissionalComDadosVinculados() throws Exception {
		// O banco recusa apagar um profissional que ainda tem contato,
		// formacao, certificado ou vinculo apontando para ele. Isso e
		// conflito de estado, nao erro interno.
		org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("fk"))
				.when(profissionalService).remover(1L);

		mockMvc.perform(delete("/profissionais/1"))
				.andExpect(status().isConflict());
	}

	@Test
	void rotaInexistenteDeveRetornar404ENao500() throws Exception {
		// Garante que o handler generico de Exception nao volte a
		// engolir as excecoes do proprio Spring.
		mockMvc.perform(get("/rota-que-nao-existe"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deveRetornar404QuandoProfissionalNaoEncontrado() throws Exception {
		when(profissionalService.buscarPorId(999L))
				.thenThrow(new RecursoNaoEncontradoException("Profissional nao encontrado: id 999"));

		mockMvc.perform(get("/profissionais/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deveListarProfissionaisPaginadosComCpfMascarado() throws Exception {
		Pageable pagina = PageRequest.of(0, 20);
		ProfissionalResumoDTO resumo = new ProfissionalResumoDTO(1L, "Maria Silva", "***.***.777-35", true);
		when(profissionalService.listar(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(resumo), pagina, 1));

		mockMvc.perform(get("/profissionais"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].nome").value("Maria Silva"))
				.andExpect(jsonPath("$.content[0].cpfMascarado").value("***.***.777-35"));
	}

	@Test
	void deveRemoverERetornar204() throws Exception {
		mockMvc.perform(delete("/profissionais/1"))
				.andExpect(status().isNoContent());

		verify(profissionalService).remover(anyLong());
	}

}
