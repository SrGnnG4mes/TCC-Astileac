package br.edu.ifac.astileac.backend.security;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.ifac.astileac.backend.config.SecurityConfig;
import br.edu.ifac.astileac.backend.controller.CertificadoController;
import br.edu.ifac.astileac.backend.service.CertificadoService;
import br.edu.ifac.astileac.backend.service.LogAcessoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Quem pode dar e retirar o visto no documento do certificado (RF13).
 *
 * So a diretoria, e nunca no proprio documento: o membro da diretoria
 * tambem e associado, e um visto dado por ele mesmo nao confere nada.
 *
 * Como o AutorizacaoTest, importa o SecurityConfig de verdade e mantem os
 * filtros: sem isso o @PreAuthorize nem seria avaliado.
 */
@WebMvcTest(CertificadoController.class)
@Import(SecurityConfig.class)
class VistoDoDocumentoAutorizacaoTest {

	private static final String VISTO = "/api/profissionais/{profissionalId}/certificados/9/validacao";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CertificadoService certificadoService;

	@MockitoBean
	private LogAcessoService logAcessoService;

	@MockitoBean(name = "autorizacao")
	private UsuarioLogado usuarioLogado;

	@Test
	@WithMockUser(roles = "ADMIN")
	void diretoriaDaVistoNoDocumentoDeOutroAssociado() throws Exception {
		when(usuarioLogado.eOProprioProfissional(2L)).thenReturn(false);

		mockMvc.perform(post(VISTO, 2L).with(csrf()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void diretorNaoDaVistoNoProprioDocumento() throws Exception {
		when(usuarioLogado.eOProprioProfissional(7L)).thenReturn(true);

		mockMvc.perform(post(VISTO, 7L).with(csrf()))
				.andExpect(status().isForbidden());

		verify(certificadoService, never()).validar(anyLong(), anyLong(), anyString());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void diretorNaoRetiraVistoDoProprioDocumento() throws Exception {
		when(usuarioLogado.eOProprioProfissional(7L)).thenReturn(true);

		mockMvc.perform(delete(VISTO, 7L).with(csrf()))
				.andExpect(status().isForbidden());

		verify(certificadoService, never()).invalidar(anyLong(), anyLong());
	}

	@Test
	@WithMockUser(roles = "PROFISSIONAL")
	void associadoNaoDaVistoNemNoProprioDocumento() throws Exception {
		when(usuarioLogado.eOProprioProfissional(7L)).thenReturn(true);

		mockMvc.perform(post(VISTO, 7L).with(csrf()))
				.andExpect(status().isForbidden());
	}
}
