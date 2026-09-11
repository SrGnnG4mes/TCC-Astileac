package br.edu.ifac.astileac.backend.dto;

/**
 * Dados de saida da LISTAGEM de profissionais. Deliberadamente menor
 * que o ProfissionalResponseDTO: traz so o necessario para localizar a
 * pessoa numa lista, com o CPF mascarado.
 *
 * Isso atende ao principio da necessidade da LGPD (Lei 13.709/2018,
 * Art. 6, III): o tratamento deve se limitar ao minimo necessario para
 * a finalidade. Achar um profissional numa lista nao exige o CPF
 * inteiro; consultar a ficha dele, sim.
 */
public record ProfissionalResumoDTO(
		Long id,
		String nome,
		String cpfMascarado,
		Boolean ativo
) {
}
