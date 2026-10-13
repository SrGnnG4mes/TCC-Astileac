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
	/** Endereco do Lattes. Nulo quando o associado nao informou. */
	lattesUrl: string | null;
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
	/** Endereco do Lattes. Vazio vira nulo no servidor. */
	lattesUrl?: string;
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

// ---------- endereco, contatos e vinculos (RF02, RF03, RF07) ----------

/** Endereco (RF02). Relacao 1:1: um profissional tem no maximo um. */
export interface Endereco {
	id: number;
	cep: string;
	logradouro: string;
	numero: string;
	complemento: string | null;
	bairro: string;
	cidade: string;
	uf: string;
}

export interface EnderecoRequest {
	cep: string;
	logradouro: string;
	numero: string;
	complemento: string;
	bairro: string;
	cidade: string;
	uf: string;
}

export type TipoContato = 'EMAIL' | 'TELEFONE';

/**
 * Contato (RF03). Um principal por TIPO: marcar um novo como principal
 * desmarca o anterior do mesmo tipo, no servidor.
 */
export interface Contato {
	id: number;
	tipo: TipoContato;
	valor: string;
	principal: boolean;
}

export interface ContatoRequest {
	tipo: TipoContato;
	valor: string;
	principal: boolean;
}

/**
 * Vinculo empregaticio (RF07).
 *
 * `atual` e DERIVADO no servidor: dataFim nula significa "em andamento".
 * Nao entra no request -- aceitar os dois deixaria o cliente gravar uma
 * contradicao.
 */
export interface Vinculo {
	id: number;
	instituicao: string;
	cargo: string;
	tipoVinculo: string;
	dataInicio: string;
	dataFim: string | null;
	atual: boolean;
}

export interface VinculoRequest {
	instituicao: string;
	cargo: string;
	tipoVinculo: string;
	dataInicio: string;
	dataFim: string | null;
}

/** Entrada da tabela de dominio de areas (RF04, manutencao pela diretoria). */
export interface AreaAtuacaoRequest {
	nome: string;
	descricao: string;
}

/**
 * Formacao academica (RF05).
 *
 * Junto com o certificado, e o que comprova a habilitacao legal exigida
 * pela Lei 14.704/2023 -- nucleo do sistema, nao cadastro acessorio.
 */
export interface Formacao {
	id: number;
	nivel: string;
	curso: string;
	instituicao: string;
	anoConclusao: number;
}

export interface FormacaoRequest {
	nivel: string;
	curso: string;
	instituicao: string;
	anoConclusao: number;
}

/**
 * Certificado (RF06).
 *
 * `formacaoId` e opcional, e quando preenchido so pode apontar para
 * formacao DO MESMO profissional -- o back-end recusa o contrario.
 *
 * `arquivoPath` nao entra no request: quem define o caminho de
 * armazenamento e o servidor, no upload (Passo 8). Deixar o cliente
 * escolher e por onde entra path traversal.
 */
export interface Certificado {
	id: number;
	formacaoId: number | null;
	titulo: string;
	instituicaoEmissora: string;
	cargaHoraria: number;
	dataEmissao: string;

	/**
	 * Presenca do documento. O caminho de armazenamento NAO vem da API:
	 * e detalhe interno do servidor, e o download tem endpoint proprio,
	 * autorizado ao ADMIN e ao titular.
	 */
	temDocumento: boolean;

	/**
	 * Reconhecimento do documento pela diretoria (RF13). Nulos enquanto
	 * ninguem reconheceu.
	 *
	 * Com `temDocumento`, formam os tres estados do certificado:
	 * declarado (sem documento), enviado (com documento, sem
	 * reconhecimento) e reconhecido.
	 */
	validadoEm: string | null;
	validadoPor: string | null;
}

export interface CertificadoRequest {
	formacaoId: number | null;
	titulo: string;
	instituicaoEmissora: string;
	cargaHoraria: number;
	dataEmissao: string;
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
// ---------- contas de acesso e convites (RF12) ----------

/**
 * Resposta da emissao -- a UNICA vez que o token aparece em texto.
 *
 * O banco guarda so o hash, entao nao ha como recupera-lo depois. Se a
 * diretoria perder o codigo, o caminho e emitir outro convite.
 */
export interface ConviteEmitido {
	id: number;
	email: string;
	token: string;
	dataExpiracao: string;
	comoEntregar: string;
}

/** Convite na listagem da diretoria. Sem token, por isso mesmo. */
export interface Convite {
	id: number;
	email: string;
	dataEmissao: string;
	dataExpiracao: string;
	dataConsumo: string | null;
	situacao: 'pendente' | 'utilizado' | 'expirado';
	profissionalId: number | null;
}

/**
 * Consulta publica do convite, antes de mostrar o formulario.
 *
 * Convite invalido nao revela se chegou a existir: a resposta e a mesma
 * para token inventado e para token ja usado.
 */
export interface ConviteValidacao {
	valido: boolean;
	email: string | null;
	motivo: string | null;
}

/**
 * Cadastro feito pelo proprio associado, com o convite em maos.
 *
 * Nao ha `dataAssociacao` nem `perfil`: a data quem define e a
 * associacao ao reconhecer o pagamento, e ninguem se promove a
 * administrador preenchendo formulario.
 */
export interface CadastroPorConvite {
	token: string;
	nome: string;
	cpf: string;
	dataNascimento: string;
	login: string;
	senha: string;
}

/** Conta de acesso na visao da diretoria. Nunca traz hash de senha. */
export interface Conta {
	id: number;
	login: string;
	perfil: Perfil;
	profissionalId: number | null;
	nomeProfissional: string | null;
}

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
