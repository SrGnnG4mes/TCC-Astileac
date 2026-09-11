package br.edu.ifac.astileac.backend.dto;

import java.time.LocalDate;

/**
 * Resposta ao pedido de eliminacao de dados (LGPD Art. 18, VI).
 *
 * Devolve a mensagem explicita, a data em que os dados serao apagados
 * e como desfazer. A transparencia aqui nao e cortesia: o Art. 6, VI
 * exige informacao clara sobre o tratamento, e uma exclusao que "some"
 * sem dizer o que aconteceu com os dados falha nesse ponto.
 *
 * O prazo vem em dois formatos de proposito: "mensagem" e para mostrar
 * a pessoa, "dataDaExclusaoDefinitiva" e para a tela calcular quanto
 * tempo resta sem precisar interpretar texto.
 */
public record ExclusaoRespostaDTO(
		String mensagem,
		LocalDate dataDoPedido,
		LocalDate dataDaExclusaoDefinitiva,
		int diasParaArrependimento,
		String comoCancelar
) {
}
