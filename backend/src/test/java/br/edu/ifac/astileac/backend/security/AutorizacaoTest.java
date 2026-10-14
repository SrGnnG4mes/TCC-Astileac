package br.edu.ifac.astileac.backend.security;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.ifac.astileac.backend.config.SecurityConfig;
import br.edu.ifac.astileac.backend.controller.ContatoController;
import br.edu.ifac.astileac.backend.service.ContatoService;
import br.edu.ifac.astileac.backend.service.LogAcessoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica as regras de acesso do RF09.
 *
 * Diferente dos outros testes de fatia web, este NAO usa
 * addFilters = false e importa o SecurityConfig de verdade: sem isso,
 * os filtros de seguranca e o method security ficam desligados e as
 * anotacoes @PreAuthorize passariam despercebidas -- o teste diria que
 * esta tudo bem sem ter verificado nada.
 *
 * O ContatoController serve de amostra: todos os controllers
 * aninhados carregam exatamente a mesma anotacao de classe.
 */
@WebMvcTest(ContatoController.class)
@Import(SecurityConfig.class)
class AutorizacaoTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ContatoService contatoService;

	@MockitoBean
	private LogAcessoService logAcessoService;

	@MockitoBean(name = "autorizacao")
	private UsuarioLogado usuarioLogado;

	@Test
	@WithAnonymousUser
	void semLoginDeveRecusarComNaoAutorizado() throws Exception {
		mockMvc.perform(get("/api/profissionais/1/contatos"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void diretoriaAlcancaQualquerProfissional() throws Exception {
		when(contatoService.listar(anyLong())).thenReturn(List.of());

		mockMvc.perform(get("/api/profissionais/1/contatos"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "PROFISSIONAL")
	void associadoAlcancaOProprioCadastro() throws Exception {
		when(usuarioLogado.eOProprioProfissional(1L)).thenReturn(true);
		when(contatoService.listar(anyLong())).thenReturn(List.of());

		mockMvc.perform(get("/api/profissionais/1/contatos"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "PROFISSIONAL")
	void associadoNaoAlcancaCadastroAlheio() throws Exception {
		// A regra que faz a LGPD valer: ter login nao basta, e preciso
		// ser o dono do registro.
		when(usuarioLogado.eOProprioProfissional(2L)).thenReturn(false);

		mockMvc.perform(get("/api/profissionais/2/contatos"))
				.andExpect(status().isForbidden());
	}

}
