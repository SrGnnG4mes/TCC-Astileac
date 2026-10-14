package br.edu.ifac.astileac.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dados de entrada de um certificado (RF06).
 *
 * NAO ha campo arquivoPath aqui, de proposito. O caminho de
 * armazenamento e definido pelo servidor no momento do upload
 * (Passo 8); deixar o cliente escolher o caminho e justamente por onde
 * entra path traversal -- um "../" bem colocado grava fora da pasta
 * prevista. Registrar o certificado e anexar o documento sao duas
 * operacoes distintas.
 *
 * formacaoId e opcional: nem todo certificado comprova uma formacao
 * registrada (cursos livres, oficinas, capacitacoes).
 */
public record CertificadoRequestDTO(
		Long formacaoId,

		@NotBlank(message = "Informe o título do certificado.")
		@Size(max = 150, message = "O título deve ter no máximo 150 caracteres.")
		String titulo,

		@NotBlank(message = "Informe a instituição emissora.")
		@Size(max = 150, message = "A instituição emissora deve ter no máximo 150 caracteres.")
		String instituicaoEmissora,

		@NotNull(message = "Informe a carga horária.")
		@Positive(message = "A carga horária deve ser maior que zero.")
		Integer cargaHoraria,

		@NotNull(message = "Informe a data de emissão.")
		@PastOrPresent(message = "A data de emissão não pode estar no futuro.")
		LocalDate dataEmissao
) {
}
