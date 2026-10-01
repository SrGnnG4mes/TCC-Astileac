package br.edu.ifac.astileac.backend.dto;

/**
 * Conta de acesso na visao da diretoria (RF12).
 *
 * Nunca traz o hash da senha -- nem ele nem qualquer derivado. Traz o
 * nome do profissional para a lista ser legivel sem uma segunda
 * consulta.
 */
public record UsuarioResponseDTO(
		Long id,
		String login,
		String perfil,
		Long profissionalId,
		String nomeProfissional
) {
}
