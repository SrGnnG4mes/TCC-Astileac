/**
 * Conteudo do site publico que depende da associacao.
 *
 * Fica num arquivo so, separado das telas, para ser preenchido sem
 * mexer em layout: quando a diretoria passar os valores e os canais de
 * contato, e aqui que eles entram. Enquanto um campo estiver nulo ou uma
 * lista vazia, a tela mostra que a informacao ainda sera publicada -- em
 * vez de um valor inventado.
 */

/** Canais de contato da diretoria. Nulo = ainda nao informado. */
export interface ContatoDaDiretoria {
	email: string | null;
	telefone: string | null;
	/** Perfil sem o @, por exemplo "astileac". */
	instagram: string | null;
}

export const CONTATO_DA_DIRETORIA: ContatoDaDiretoria = {
	email: null,
	telefone: null,
	instagram: null
};

/** Uma linha da tabela de valores de referencia. */
export interface ItemDeValor {
	servico: string;
	/** Texto livre: "R$ 150,00", "a partir de R$ 90,00", "a combinar". */
	valor: string;
	/** Por hora, por evento, por minuto de video... */
	unidade: string;
	observacao?: string;
}

/** Agrupamento da tabela, por exemplo "Interpretação" e "Tradução". */
export interface GrupoDeValores {
	titulo: string;
	itens: ItemDeValor[];
}

/**
 * Valores de referencia. Vazio ate a diretoria definir.
 *
 * Exemplo do formato:
 *   {
 *     titulo: 'Interpretação',
 *     itens: [
 *       { servico: 'Interpretação em evento', valor: 'R$ 000,00', unidade: 'por hora' }
 *     ]
 *   }
 */
export const VALORES_DE_REFERENCIA: GrupoDeValores[] = [];

/** Ano ou data de vigencia da tabela, por exemplo "2026". */
export const VIGENCIA_DOS_VALORES: string | null = null;
