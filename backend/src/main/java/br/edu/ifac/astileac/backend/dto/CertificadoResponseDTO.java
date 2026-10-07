package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Certificado (RF06). formacaoId e opcional: nem todo certificado
 * comprova uma formacao registrada.
 *
 * O caminho do arquivo NAO sai daqui: o cliente so precisa saber se ha
 * documento, e baixa-lo por endpoint proprio, autorizado ao ADMIN e ao
 * titular. Devolver o caminho entregaria a estrutura de pastas do
 * servidor sem nenhum ganho.
 */
public record CertificadoResponseDTO(
		Long id,
		Long formacaoId,
		String titulo,
		String instituicaoEmissora,
		Integer cargaHoraria,
		LocalDate dataEmissao,

		/**
		 * Presenca do documento, nao o caminho.
		 *
		 * O caminho de armazenamento e detalhe interno do servidor e nao
		 * acrescenta nada ao cliente: para baixar existe um endpoint
		 * proprio. Expor a estrutura de pastas so entrega informacao a
		 * quem fosse procurar por onde atacar.
		 */
		boolean temDocumento,

		/** Nulos enquanto a diretoria nao reconheceu o documento (RF13). */
		LocalDateTime validadoEm,
		String validadoPor
) {
}
