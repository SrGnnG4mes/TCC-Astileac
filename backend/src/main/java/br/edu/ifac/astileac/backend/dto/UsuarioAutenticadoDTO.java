package br.edu.ifac.astileac.backend.dto;

import br.edu.ifac.astileac.backend.security.Perfil;

/**
 * Quem esta logado. O front-end usa isto para decidir o que mostrar --
 * a diretoria ve a busca do quadro de associados, o associado ve so o
 * proprio cadastro.
 *
 * Nao devolve nada sensivel: nem senha (obviamente), nem CPF. E so
 * identidade e permissao.
 */
public record UsuarioAutenticadoDTO(
		String login,
		Perfil perfil,
		Long profissionalId
) {
}
