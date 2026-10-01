/**
 * Tipos que espelham os DTOs da API.
 *
 * Sao escritos a mao de proposito: o contrato da API e o que o back-end
 * publica, e manter o espelho explicito aqui faz qualquer divergencia
 * aparecer na compilacao do TypeScript, e nao em tempo de execucao.
 */

export type Perfil = 'ADMIN' | 'PROFISSIONAL';

/** GET /autenticacao/eu -- identidade e permissao, nada sensivel. */
export interface UsuarioAutenticado {
	login: string;
	perfil: Perfil;
	profissionalId: number | null;
}

/**
 * Item da LISTAGEM (RF08). O CPF vem mascarado porque a listagem nao
 * precisa do numero inteiro -- principio da necessidade, Art. 6, III da
 * LGPD. O numero completo so existe no ProfissionalResponseDTO, na
 * consulta individual.
 */
export interface ProfissionalResumo {
	id: number;
	nome: string;
	cpfMascarado: string;
	ativo: boolean;
}

/**
 * Ficha individual. Aqui o CPF vem INTEIRO -- a consulta individual tem
 * finalidade que justifica o dado completo, ao contrario da listagem.
 *
 * `dataExclusaoSolicitada` preenchida significa cadastro bloqueado por
 * pedido de eliminacao (RF10, Art. 18 da LGPD), dentro da janela de 30
 * dias em que o pedido ainda pode ser desfeito.
 */
export interface ProfissionalFicha {
	id: number;
	nome: string;
	cpf: string;
	dataNascimento: string;
	dataAssociacao: string;
	ativo: boolean;
	dataExclusaoSolicitada: string | null;
}

/** Tabela de dominio das areas de atuacao. */
export interface AreaAtuacao {
	id: number;
	nome: string;
	descricao: string;
}

/**
 * Resposta paginada do Spring Data.
 *
 * ATENCAO ao formato: os metadados vem ANINHADOS em `page`, nao no topo
 * do objeto. Isso e consequencia do PageSerializationMode.VIA_DTO
 * configurado no WebConfig -- o formato estavel, em vez da serializacao
 * direta do PageImpl, que o Spring avisa nao ser garantida.
 *
 * Confirmado contra a API rodando:
 *   {"content":[...],"page":{"size":20,"number":0,
 *    "totalElements":1,"totalPages":1}}
 *
 * Declarar os campos no topo compila sem erro e falha em silencio: as
 * propriedades chegam como undefined e a tela mostra "NaN".
 */
export interface Pagina<T> {
	content: T[];
	page: {
		size: number;
		number: number;
		totalElements: number;
		totalPages: number;
	};
}

/** Corpo de erro padronizado pelo GlobalExceptionHandler. */
export interface ErroResposta {
	status: number;
	mensagem: string;
}

/** Filtros do RF08. Todos opcionais: ausente significa "nao filtre". */
export interface FiltroProfissional {
	nome?: string;
	area?: string;
	ativo?: boolean;
	page?: number;
	size?: number;
}
