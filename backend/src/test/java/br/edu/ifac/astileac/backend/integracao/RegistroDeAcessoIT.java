package br.edu.ifac.astileac.backend.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import br.edu.ifac.astileac.backend.entity.Usuario;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.security.Perfil;
import br.edu.ifac.astileac.backend.security.UsuarioDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Registro de acesso (RF11, Art. 46) de ponta a ponta: requisicao HTTP,
 * interceptor, banco e a consulta que o titular faz.
 *
 * O interceptor deduz a entidade e o id do proprio caminho da URL, e o
 * titular so ve o que foi gravado com a entidade certa. Se o formato do
 * caminho mudar e o interceptor nao acompanhar, nada quebra: a gravacao
 * continua acontecendo, so que com a entidade errada, e o registro do
 * titular passa a voltar vazio -- em silencio. E isso que este teste
 * vigia.
 */
@AutoConfigureMockMvc
class RegistroDeAcessoIT extends IntegracaoBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Associados associados;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioDetailsService usuarioDetailsService;

	/**
	 * A diretora entra com uma conta de verdade, carregada do banco, e nao
	 * com @WithMockUser: o autor do registro sai do principal da sessao, e
	 * o usuario simulado do Spring nao e do tipo que o sistema reconhece --
	 * o registro sairia como "nao-autenticado". Descoberto na primeira
	 * execucao deste teste.
	 */
	@Test
	void consultarAFichaApareceNoRegistroDoTitular() throws Exception {
		Long id = associados.porConvite("Maria Silva", "11144477735", "maria.silva");
		UserDetails diretora = contaAdministrativa("diretora");

		mockMvc.perform(get("/api/profissionais/{id}", id).with(user(diretora))).andExpect(status().isOk());

		assertEquals(1, contar("""
				SELECT count(*) FROM log_acesso
				WHERE entidade = 'PROFISSIONAIS' AND registro_id = ?
				  AND acao = 'CONSULTA' AND autor = 'diretora'
				""", id));

		mockMvc.perform(get("/api/profissionais/{id}/dados-pessoais/registro-de-acesso", id).with(user(diretora)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[?(@.autor == 'diretora' && @.acao == 'CONSULTA')]").exists());
	}

	private UserDetails contaAdministrativa(String login) {
		Usuario conta = new Usuario();
		conta.setLogin(login);
		conta.setSenhaHash("hash-irrelevante-para-este-teste");
		conta.setPerfil(Perfil.ADMIN.name());
		usuarioRepository.save(conta);
		return usuarioDetailsService.loadUserByUsername(login);
	}

}
