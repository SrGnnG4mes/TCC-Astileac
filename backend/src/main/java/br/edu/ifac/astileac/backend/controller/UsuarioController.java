package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.PerfilRequestDTO;
import br.edu.ifac.astileac.backend.dto.SenhaRequestDTO;
import br.edu.ifac.astileac.backend.dto.UsuarioResponseDTO;
import br.edu.ifac.astileac.backend.security.UsuarioLogado;
import br.edu.ifac.astileac.backend.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contas de acesso existentes (RF12).
 *
 * Criar conta nao esta aqui: contas nascem pelo convite, no
 * CadastroController. Aqui ficam a administracao do perfil e a troca da
 * propria senha.
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

	private final UsuarioService usuarioService;
	private final UsuarioLogado usuarioLogado;

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public List<UsuarioResponseDTO> listar() {
		return usuarioService.listar();
	}

	/**
	 * Concede ou retira o perfil administrativo.
	 *
	 * Recusa a operacao que deixaria a associacao sem nenhum
	 * administrador -- so administrador mexe em conta de administrador,
	 * e sair dessa situacao exigiria alterar o banco na mao.
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/perfil")
	public UsuarioResponseDTO alterarPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequestDTO dto) {
		return usuarioService.alterarPerfil(id, dto.perfil());
	}

	/**
	 * Troca a senha de quem esta logado.
	 *
	 * Caminho fixo "eu", sem id: assim nao existe a forma de pedir a
	 * troca para outra conta, nem por engano nem de proposito. Exige a
	 * senha atual.
	 */
	@PostMapping("/eu/senha")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void trocarPropriaSenha(@Valid @RequestBody SenhaRequestDTO dto) {
		usuarioService.trocarPropriaSenha(usuarioLogado.loginAtual(), dto.senhaAtual(), dto.senhaNova());
	}
}
