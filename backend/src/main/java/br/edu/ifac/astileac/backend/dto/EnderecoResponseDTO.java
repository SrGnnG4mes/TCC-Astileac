package br.edu.ifac.astileac.backend.dto;

/**
 * Dados de saida do endereco. Nao repete os dados do profissional: o
 * endereco e sempre acessado a partir dele
 * (/profissionais/{id}/endereco), entao quem chamou ja sabe de quem e.
 */
public record EnderecoResponseDTO(
		Long id,
		String cep,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String cidade,
		String uf
) {
}
