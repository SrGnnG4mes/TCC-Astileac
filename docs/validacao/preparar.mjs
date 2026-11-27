// Prepara a sessao de validacao com a diretoria (roteiro de validacao):
// cadastros FICTICIOS para a consulta e as contas de quem vai usar o sistema.
//
// Uso, com o back-end da validacao no ar e o banco ainda vazio:
//     ASTILEAC_ADMIN_LOGIN=instalacao ASTILEAC_ADMIN_SENHA=... \
//         node docs/validacao/preparar.mjs [--url http://127.0.0.1:8080] [--contas 4]
//
// Tudo passa pela API, como uma pessoa faria pelas telas: as regras do
// sistema valem para estes dados como para quaisquer outros, e as contas
// nascem pelo convite (RF12), com a senha guardada em BCrypt pelo servidor.
//
// As senhas provisorias vao para docs/validacao/contas-da-sessao.txt, que
// esta no .gitignore. Nenhum dado aqui e de pessoa real: nomes inventados,
// CPFs gerados, e-mails em example.org (dominio reservado para exemplos).

import { randomBytes, randomInt } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const PASTA = dirname(fileURLToPath(import.meta.url));
const PDF_DE_EXEMPLO = join(PASTA, '..', '..', 'Postman', 'certificado-exemplo.pdf');
const ARQUIVO_DAS_CONTAS = join(PASTA, 'contas-da-sessao.txt');

const argumentos = process.argv.slice(2);
const opcao = (nome, padrao) => {
	const i = argumentos.indexOf(`--${nome}`);
	return i >= 0 ? argumentos[i + 1] : padrao;
};
const API = `${opcao('url', 'http://127.0.0.1:8080')}/api`;
const QUANTAS_CONTAS = Number(opcao('contas', '4'));
const ADMIN_LOGIN = process.env.ASTILEAC_ADMIN_LOGIN;
const ADMIN_SENHA = process.env.ASTILEAC_ADMIN_SENHA;

// ---------- sessao HTTP: cookie de sessao e token CSRF ----------

/**
 * Uma sessao do navegador, em miniatura: guarda os cookies e, antes de
 * cada escrita, le o token CSRF -- a mesma regra que o front-end segue.
 */
class Sessao {
	cookies = new Map();

	async pedir(metodo, caminho, corpo) {
		const cabecalhos = { Cookie: [...this.cookies].map(([n, v]) => `${n}=${v}`).join('; ') };
		if (metodo !== 'GET') {
			cabecalhos['X-XSRF-TOKEN'] = await this.tokenCsrf();
			cabecalhos.Cookie = [...this.cookies].map(([n, v]) => `${n}=${v}`).join('; ');
		}
		let body;
		if (corpo instanceof FormData || corpo instanceof URLSearchParams) {
			body = corpo;
		} else if (corpo !== undefined) {
			body = JSON.stringify(corpo);
			cabecalhos['Content-Type'] = 'application/json';
		}
		const resposta = await fetch(`${API}${caminho}`, { method: metodo, headers: cabecalhos, body });
		this.guardarCookies(resposta);
		const texto = await resposta.text();
		if (!resposta.ok) {
			let mensagem = texto;
			try { mensagem = JSON.parse(texto).mensagem ?? texto; } catch { /* corpo nao e JSON */ }
			throw new Error(`${metodo} ${caminho} -> ${resposta.status}: ${mensagem}`);
		}
		return texto ? JSON.parse(texto) : null;
	}

	async tokenCsrf() {
		const resposta = await fetch(`${API}/autenticacao/csrf`, {
			headers: { Cookie: [...this.cookies].map(([n, v]) => `${n}=${v}`).join('; ') }
		});
		this.guardarCookies(resposta);
		return this.cookies.get('XSRF-TOKEN');
	}

	guardarCookies(resposta) {
		for (const cookie of resposta.headers.getSetCookie()) {
			const [par] = cookie.split(';');
			const [nome, ...valor] = par.split('=');
			this.cookies.set(nome.trim(), valor.join('='));
		}
	}

	async entrar(login, senha) {
		await this.pedir('POST', '/autenticacao/login', new URLSearchParams({ login, senha }));
	}
}

// ---------- geradores ----------

const cpfsUsados = new Set();

/** CPF com digitos verificadores validos, sorteado e sem repetir. */
function gerarCpf() {
	for (;;) {
		const n = Array.from({ length: 9 }, () => randomInt(10));
		for (const peso of [10, 11]) {
			const soma = n.reduce((total, d, i) => total + d * (peso - i), 0);
			const resto = (soma * 10) % 11;
			n.push(resto === 10 ? 0 : resto);
		}
		const cpf = n.join('');
		if (!/^(\d)\1{10}$/.test(cpf) && !cpfsUsados.has(cpf)) {
			cpfsUsados.add(cpf);
			return cpf;
		}
	}
}

/** Senha provisoria: 12 caracteres, trocada pela pessoa na primeira tarefa. */
const gerarSenha = () => randomBytes(9).toString('base64url');

// ---------- os cadastros ficticios ----------

/*
 * Pensados para o roteiro: a consulta da area juridica (tarefa C1) acha
 * mais de uma pessoa, uma delas atua em duas areas e aparece uma vez so, e
 * ha uma inativa para o filtro de situacao. Os tres estados do documento do
 * certificado aparecem nos dados.
 */
const PROFISSIONAIS = [
	{
		nome: 'Ana Paula Ribeiro', nascimento: '1986-04-12', associacao: '2019-03-10',
		lattes: 'http://lattes.cnpq.br/0000000000000001',
		email: 'ana.ribeiro@example.org', telefone: '68999110001',
		endereco: ['69900000', 'Rua Benjamin Constant', '845', 'Apto 12', 'Centro'],
		areas: [['juridica', '2019-04-01'], ['educacional', '2015-02-01']],
		formacoes: [['Graduação', 'Letras: Língua Brasileira de Sinais', 'Universidade Federal do Acre', 2013]],
		certificados: [['Exame de proficiência em tradução e interpretação', 'Ministério da Educação', 40, '2014-11-20', 0, 'visto']],
		vinculos: [['Tribunal de Justiça do Acre', 'Intérprete judicial', 'Efetivo', '2018-05-02', null]]
	},
	{
		nome: 'Bruno Cavalcante Lima', nascimento: '1990-08-03', associacao: '2020-07-15',
		email: 'bruno.lima@example.org', telefone: '68999110002',
		endereco: ['69911000', 'Avenida Ceará', '1200', null, 'Cadeia Velha'],
		areas: [['juridica', '2020-08-01']],
		formacoes: [['Especialização', 'Tradução e Interpretação de Língua Brasileira de Sinais', 'Instituto Federal do Acre', 2018]],
		certificados: [['Interpretação em contextos jurídicos', 'Escola do Poder Judiciário', 60, '2021-06-30', 0, 'anexado']],
		vinculos: [['Defensoria Pública do Estado do Acre', 'Intérprete', 'Contrato temporário', '2021-01-10', '2023-12-20']]
	},
	{
		nome: 'Carla Menezes Duarte', nascimento: '1982-01-25', associacao: '2016-02-01', inativa: true,
		email: 'carla.duarte@example.org', telefone: '68999110003',
		areas: [['juridica', '2016-03-01']],
		formacoes: [['Graduação', 'Pedagogia', 'Universidade Federal do Acre', 2008]],
		certificados: [['Curso de intérprete de Língua Brasileira de Sinais', 'Centro de Apoio ao Surdo', 180, '2010-12-15', 0, null]],
		vinculos: []
	},
	{
		nome: 'Daniel Freitas Moura', nascimento: '1994-10-09', associacao: '2022-05-20',
		email: 'daniel.moura@example.org', telefone: '68999110004',
		endereco: ['69918000', 'Rua Rio de Janeiro', '33', null, 'Estação Experimental'],
		areas: [['saude', '2022-06-01']],
		formacoes: [['Graduação', 'Letras: Língua Brasileira de Sinais', 'Universidade Federal do Acre', 2019]],
		certificados: [['Interpretação em ambientes de saúde', 'Secretaria de Estado de Saúde', 40, '2022-09-05', 0, 'anexado']],
		vinculos: [['Hospital de Urgência e Emergência de Rio Branco', 'Intérprete', 'Contrato temporário', '2023-02-01', null]]
	},
	{
		nome: 'Elisa Martins Barros', nascimento: '1988-06-17', associacao: '2018-08-08',
		lattes: 'http://lattes.cnpq.br/0000000000000005',
		email: 'elisa.barros@example.org', telefone: '68999110005',
		endereco: ['69900000', 'Travessa Guaporé', '77', 'Casa 2', 'Bosque'],
		areas: [['educacional', '2018-09-01']],
		formacoes: [
			['Graduação', 'Letras: Língua Brasileira de Sinais', 'Universidade Federal do Acre', 2012],
			['Mestrado', 'Educação', 'Universidade Federal do Acre', 2017]
		],
		certificados: [['Tradução e interpretação no ensino superior', 'Universidade Federal do Acre', 120, '2016-07-01', 0, 'visto']],
		vinculos: [['Secretaria de Estado de Educação do Acre', 'Tradutora e intérprete', 'Efetivo', '2014-03-01', null]]
	},
	{
		nome: 'Fábio Teixeira Sales', nascimento: '1979-12-02', associacao: '2015-05-05',
		email: 'fabio.sales@example.org', telefone: '68999110006',
		areas: [['artistico-cultural', '2015-06-01']],
		formacoes: [['Graduação', 'Artes Cênicas', 'Universidade Federal do Acre', 2004]],
		certificados: [['Interpretação artística em Língua Brasileira de Sinais', 'Fundação de Cultura Elias Mansour', 80, '2016-10-10', 0, null]],
		vinculos: []
	},
	{
		nome: 'Gabriela Rocha Prado', nascimento: '1992-03-28', associacao: '2021-01-12',
		email: 'gabriela.prado@example.org', telefone: '68999110007',
		endereco: ['69915000', 'Rua Alvorada', '410', null, 'Conjunto Universitário'],
		areas: [['educacional', '2021-02-01'], ['saude', '2022-03-01']],
		formacoes: [['Graduação', 'Letras: Língua Brasileira de Sinais', 'Universidade Federal do Acre', 2016]],
		certificados: [],
		vinculos: [['Instituto Federal do Acre', 'Tradutora e intérprete', 'Efetivo', '2019-08-01', null]]
	},
	{
		nome: 'Hugo Almeida Castro', nascimento: '1985-09-14', associacao: '2017-11-30',
		email: 'hugo.castro@example.org', telefone: '68999110008',
		areas: [['outras', '2017-12-01']],
		formacoes: [['Técnico', 'Tradução e Interpretação de Língua Brasileira de Sinais', 'Instituto Federal do Acre', 2015]],
		certificados: [['Guia-interpretação para pessoas surdocegas', 'Associação de Surdocegos', 100, '2019-04-22', 0, 'anexado']],
		vinculos: []
	},
	{
		nome: 'Isabela Pinto Queiroz', nascimento: '1996-02-08', associacao: '2023-03-03',
		email: 'isabela.queiroz@example.org', telefone: '68999110009',
		endereco: ['69900000', 'Rua Marechal Deodoro', '1500', 'Sala 3', 'Centro'],
		areas: [['saude', '2023-04-01'], ['juridica', '2024-02-01']],
		formacoes: [['Graduação', 'Letras: Língua Brasileira de Sinais', 'Universidade Federal do Acre', 2021]],
		certificados: [['Interpretação em audiências', 'Escola do Poder Judiciário', 40, '2024-05-15', 0, null]],
		vinculos: []
	},
	{
		nome: 'João Victor Farias', nascimento: '1991-11-21', associacao: '2020-10-10',
		email: 'joao.farias@example.org', telefone: '68999110010',
		areas: [['educacional', '2020-11-01']],
		formacoes: [['Especialização', 'Educação de Surdos', 'Universidade Federal do Acre', 2019]],
		certificados: [['Interpretação educacional', 'Secretaria Municipal de Educação', 120, '2020-03-12', 0, 'anexado']],
		vinculos: [['Secretaria Municipal de Educação de Rio Branco', 'Intérprete educacional', 'Contrato temporário', '2020-02-10', null]]
	}
];

// ---------- execucao ----------

async function criarProfissional(admin, p, areaPorNome, pdf) {
	const criado = await admin.pedir('POST', '/profissionais', {
		nome: p.nome, cpf: gerarCpf(), dataNascimento: p.nascimento, dataAssociacao: p.associacao,
		lattesUrl: p.lattes ?? ''
	});
	const base = `/profissionais/${criado.id}`;

	await admin.pedir('POST', `${base}/contatos`, { tipo: 'EMAIL', valor: p.email, principal: true });
	await admin.pedir('POST', `${base}/contatos`, { tipo: 'TELEFONE', valor: p.telefone, principal: true });

	if (p.endereco) {
		const [cep, logradouro, numero, complemento, bairro] = p.endereco;
		await admin.pedir('PUT', `${base}/endereco`,
			{ cep, logradouro, numero, complemento, bairro, cidade: 'Rio Branco', uf: 'AC' });
	}
	for (const [nome, dataInicio] of p.areas) {
		await admin.pedir('POST', `${base}/areas`, { areaAtuacaoId: areaPorNome.get(nome), dataInicio });
	}
	const formacoes = [];
	for (const [nivel, curso, instituicao, anoConclusao] of p.formacoes) {
		formacoes.push(await admin.pedir('POST', `${base}/formacoes`, { nivel, curso, instituicao, anoConclusao }));
	}
	for (const [titulo, instituicaoEmissora, cargaHoraria, dataEmissao, formacao, documento] of p.certificados) {
		const certificado = await admin.pedir('POST', `${base}/certificados`, {
			titulo, instituicaoEmissora, cargaHoraria, dataEmissao, formacaoId: formacoes[formacao]?.id ?? null
		});
		if (documento) {
			const envio = new FormData();
			envio.append('arquivo', new Blob([pdf], { type: 'application/pdf' }), 'certificado.pdf');
			await admin.pedir('POST', `${base}/certificados/${certificado.id}/arquivo`, envio);
		}
		if (documento === 'visto') {
			await admin.pedir('POST', `${base}/certificados/${certificado.id}/validacao`);
		}
	}
	for (const [instituicao, cargo, tipoVinculo, dataInicio, dataFim] of p.vinculos) {
		await admin.pedir('POST', `${base}/vinculos`, { instituicao, cargo, tipoVinculo, dataInicio, dataFim });
	}
	if (p.inativa) {
		await admin.pedir('PUT', `${base}/situacao`, { ativo: false });
	}
}

/**
 * Conta de um membro da diretoria: nasce pelo convite, como a de qualquer
 * associado (o diretor tambem e associado), e recebe o perfil depois.
 */
async function criarContaDaDiretoria(admin, numero) {
	const login = `diretoria${numero}`;
	const senha = gerarSenha();
	const convite = await admin.pedir('POST', '/convites', { email: `${login}@example.org` });

	const visitante = new Sessao();
	await visitante.pedir('POST', '/cadastro', {
		token: convite.token, nome: `Membro da Diretoria ${numero}`, cpf: gerarCpf(),
		dataNascimento: `198${numero % 10}-0${(numero % 9) + 1}-15`, login, senha
	});

	const conta = (await admin.pedir('GET', '/usuarios')).find(u => u.login === login);
	await admin.pedir('PUT', `/usuarios/${conta.id}/perfil`, { perfil: 'ADMIN' });
	return { login, senha };
}

async function principal() {
	if (!ADMIN_LOGIN || !ADMIN_SENHA) {
		throw new Error('Informe ASTILEAC_ADMIN_LOGIN e ASTILEAC_ADMIN_SENHA (a conta de instalação).');
	}
	const admin = new Sessao();
	await admin.entrar(ADMIN_LOGIN, ADMIN_SENHA);

	// Nunca em cima de dados existentes: o roteiro pede um banco so com
	// dados ficticios, e rodar duas vezes duplicaria tudo.
	const existentes = await admin.pedir('GET', '/profissionais?size=1');
	if (existentes.page.totalElements > 0) {
		throw new Error(`O banco já tem ${existentes.page.totalElements} cadastro(s). Use um banco vazio, só para a validação.`);
	}

	const areaPorNome = new Map((await admin.pedir('GET', '/areas-atuacao')).map(a => [a.nome, a.id]));
	const pdf = readFileSync(PDF_DE_EXEMPLO);

	for (const p of PROFISSIONAIS) {
		await criarProfissional(admin, p, areaPorNome, pdf);
		console.log(`cadastro ficticio: ${p.nome}`);
	}

	const contas = [];
	for (let i = 1; i <= QUANTAS_CONTAS; i++) {
		contas.push(await criarContaDaDiretoria(admin, i));
		console.log(`conta da diretoria: diretoria${i}`);
	}

	const linhas = [
		'Contas da sessão de validação (dados fictícios). NÃO vai para o Git.',
		`Gerado em ${new Date().toLocaleString('pt-BR')}.`,
		'',
		'Uma conta por membro da diretoria. A senha é provisória: a primeira',
		'tarefa do roteiro (A2) é trocá-la em "Alterar senha".',
		'',
		...contas.map(c => `  ${c.login.padEnd(12)} ${c.senha}`),
		'',
		'Conta de instalação (só para preparar; não usar na sessão):',
		`  ${ADMIN_LOGIN.padEnd(12)} ${ADMIN_SENHA}`,
		''
	];
	writeFileSync(ARQUIVO_DAS_CONTAS, linhas.join('\n'), 'utf8');
	console.log(`\n${PROFISSIONAIS.length} cadastros e ${contas.length} contas. Senhas em ${ARQUIVO_DAS_CONTAS}`);
}

principal().catch(falha => {
	console.error(`FALHOU: ${falha.message}`);
	process.exit(1);
});
