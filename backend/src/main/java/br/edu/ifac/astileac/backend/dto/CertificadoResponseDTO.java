package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;

/**
 * Certificado (RF06). formacaoId e opcional: nem todo certificado
 * comprova uma formacao registrada.
 *
 * arquivoPath e o caminho interno do arquivo, nao uma URL publica --
 * servir certificado por endereco adivinhavel e o risco tratado no
 * Passo 8.
 */
public record CertificadoResponseDTO(
		Long id,
		Long formacaoId,
		String titulo,
		String instituicaoEmissora,
		Integer cargaHoraria,
		LocalDate dataEmissao,
		String arquivoPath
) {
}
