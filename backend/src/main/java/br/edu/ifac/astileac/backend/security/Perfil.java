package br.edu.ifac.astileac.backend.security;

/**
 * Perfis de acesso (RF09).
 *
 * Sao dois de proposito. ADMIN e a diretoria, que administra o
 * cadastro; PROFISSIONAL e o associado, que so alcanca os proprios
 * dados. E o segundo perfil que atende a LGPD de verdade -- so ter
 * login, sem separar quem ve o que, nao protege ninguem.
 */
public enum Perfil {
	ADMIN,
	PROFISSIONAL;

	/**
	 * O Spring Security espera a autoridade prefixada com "ROLE_"
	 * quando se usa hasRole(...). O banco guarda so o nome do perfil.
	 */
	public String comoAutoridade() {
		return "ROLE_" + name();
	}
}
