package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Tudo que o sistema guarda sobre uma pessoa, em um documento so
 * (LGPD Art. 18, II e V: acesso e portabilidade).
 *
 * Precisa ser COMPLETO: uma exportacao que omite parte dos dados nao
 * cumpre o direito. Por isso inclui formacoes, certificados e vinculos
 * mesmo antes de esses cadastros terem endpoints proprios -- os dados
 * ja podem existir no banco.
 *
 * Aqui o CPF vai inteiro, ao contrario da listagem: o destinatario e o
 * proprio titular, e mascarar o dado dele para ele mesmo nao protege
 * ninguem.
 */
public record DadosPessoaisDTO(
		LocalDate dataDaExportacao,
		IdentificacaoDTO identificacao,
		EnderecoResponseDTO endereco,
		List<ContatoResponseDTO> contatos,
		List<ProfissionalAreaResponseDTO> areasDeAtuacao,
		List<FormacaoResponseDTO> formacoes,
		List<CertificadoResponseDTO> certificados,
		List<VinculoEmpregaticioResponseDTO> vinculosEmpregaticios
) {

	public record IdentificacaoDTO(
			Long id,
			String nome,
			String cpf,
			LocalDate dataNascimento,
			LocalDate dataAssociacao,
			Boolean ativo
	) {
	}

}
