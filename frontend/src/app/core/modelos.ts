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
 * Entrada de cadastro (RF01).
 *
 * `ativo` NAO entra aqui, e isso e deliberado no back-end: profissional
 * novo nasce ativo, e desativar e acao propria -- nao campo livre do
 * formulario.
 */
export interface ProfissionalRequest {
	nome: string;
	cpf: string;
	dataNascimento: string;
	dataAssociacao: string;
}

/** Vinculo entre profissional e area (RF04), com data de inicio. */
export interface ProfissionalArea {
	areaAtuacaoId: number;
	nome: string;
	dataInicio: string;
}

export interface ProfissionalAreaRequest {
	areaAtuacaoId: number;
	dataInicio: string;
}

/**
 * Exportacao completa dos dados do titular (RF10, Art. 18 da LGPD).
 *
 * Traz o CPF inteiro: o destinatario e a propria pessoa, e mascarar
 * para ela mesma nao protegeria ninguem.
 */
export interface DadosPessoais {
	dataDaExportacao: string;
	identificacao: {
		id: number;
		nome: string;
		cpf: string;
		dataNascimento: string;
		dataAssociacao: string;
		ativo: boolean;
	};
	endereco: unknown | null;
	contatos: unknown[];
	areasDeAtuacao: ProfissionalArea[];
	formacoes: unknown[];
	certificados: unknown[];
	vinculosEmpregaticios: unknown[];
}

/**
 * Resposta do pedido de eliminacao.
 *
 * Nao e 204 sem corpo de proposito: a pessoa precisa saber que os dados
 * ficam por 30 dias, ate quando, e como desfazer.
 */
export interface ExclusaoResposta {
	mensagem: string;
	dataDoPedido: string;
	dataDaExclusaoDefinitiva: string;
	diasParaArrependimento: number;
	comoCancelar: string;
}

/**
 * Conferido contra o enum AcaoLog do back-end. EXPORTACAO e uma acao
 * propria, distinta de CONSULTA: exportar nao e so ler, e entregar o
 * conjunto inteiro ao titular (Art. 18).
 */
export type AcaoLog = 'CONSULTA' | 'CRIACAO' | 'ALTERACAO' | 'EXCLUSAO' | 'EXPORTACAO';

/** Registro de acesso a dado pessoal (RF11, Art. 46). */
export interface LogAcesso {
	id: number;
	dataHora: string;
	autor: string;
	/** Nulo quando a acao nao veio de uma requisicao HTTP. */
	origem: string | null;
	acao: AcaoLog;
	entidade: string;
	registroId: number | null;
	detalhe: string;
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
