// Gera as imagens dos diagramas a partir dos fontes Mermaid (.mmd).
//
// Uso, na raiz do repositorio:
//     node docs/diagramas/gerar.mjs
//
// Precisa de Node e de um Chrome instalado: o mermaid-cli desenha no
// navegador. Se o Chrome nao estiver no caminho padrao, informe-o em
// PUPPETEER_EXECUTABLE_PATH.
//
// O DER completo (docs/ASTILEAC-BD-atual.mmd) e a fonte de verdade do
// modelo. As duas vistas parciais (der-cadastro e der-acesso) existem so
// porque o DER inteiro, numa pagina A4, fica com letra pequena demais para
// ler impresso. Elas sao GERADAS aqui, copiando as tabelas do DER completo,
// para nunca divergirem dele: altere sempre o completo.

import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const PASTA = dirname(fileURLToPath(import.meta.url));
const DER_COMPLETO = join(PASTA, '..', 'ASTILEAC-BD-atual.mmd');
const CONFIG = join(PASTA, 'mermaid-config.json');
const MERMAID_CLI = '@mermaid-js/mermaid-cli@11.4.2';

// ---------- vistas parciais do DER ----------

const der = readFileSync(DER_COMPLETO, 'utf8').replace(/\r\n/g, '\n');

const tabelas = {};
for (const bloco of der.matchAll(/^\t([A-Z_]+) \{\n[\s\S]*?^\t\}\n/gm)) {
	tabelas[bloco[1]] = bloco[0];
}
const relacoes = der.split('\n').filter(linha => /^\t[A-Z_]+ \S+ [A-Z_]+ : /.test(linha));

function vista(titulo, nomes, extras) {
	const faltando = nomes.filter(nome => !tabelas[nome]);
	if (faltando.length) {
		throw new Error(`Tabela nao encontrada no DER completo: ${faltando.join(', ')}`);
	}
	return [
		'erDiagram',
		`\t%% ${titulo}`,
		'\t%% GERADO por gerar.mjs a partir de docs/ASTILEAC-BD-atual.mmd. Nao edite:',
		'\t%% altere o DER completo e rode o script de novo.',
		'\tdirection TB',
		...nomes.map(nome => tabelas[nome]),
		...extras
	].join('\n') + '\n';
}

const CADASTRO = ['PROFISSIONAL', 'ENDERECO', 'CONTATO', 'AREA_ATUACAO', 'PROFISSIONAL_AREA',
	'FORMACAO', 'CERTIFICADO', 'VINCULO_EMPREGATICIO'];

writeFileSync(join(PASTA, 'der-cadastro.mmd'), vista(
	'Vista parcial do DER: o cadastro do profissional.',
	CADASTRO,
	relacoes.filter(linha => {
		const [origem, , destino] = linha.trim().split(' ');
		return CADASTRO.includes(origem) && CADASTRO.includes(destino);
	})
));

// Nesta vista PROFISSIONAL aparece so pela chave, e as relacoes sao escritas
// a mao para a conta de acesso ficar embaixo e o convite em cima.
writeFileSync(join(PASTA, 'der-acesso.mmd'), vista(
	'Vista parcial do DER: acesso e protecao de dados.',
	['USUARIO', 'CONVITE', 'LOG_ACESSO'],
	[
		'\tPROFISSIONAL {',
		'\t\tbigint id PK "demais colunas na vista do cadastro"',
		'\t}',
		'\tCONVITE |o..o| PROFISSIONAL : "originou"',
		'\tPROFISSIONAL |o..o| USUARIO : "acessa por"'
	]
));

// ---------- imagens ----------

const chrome = process.env.PUPPETEER_EXECUTABLE_PATH
	?? ['C:/Program Files/Google/Chrome/Application/chrome.exe',
		'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
		'/usr/bin/google-chrome'].find(caminho => existsSync(caminho));

const argsPuppeteer = [];
if (chrome) {
	const configPuppeteer = join(mkdtempSync(join(tmpdir(), 'diagramas-')), 'puppeteer.json');
	writeFileSync(configPuppeteer, JSON.stringify({ executablePath: chrome, headless: 'new' }));
	argsPuppeteer.push('-p', configPuppeteer);
}

const fontes = [
	DER_COMPLETO,
	...readdirSync(PASTA).filter(nome => nome.endsWith('.mmd')).map(nome => join(PASTA, nome))
];

let falhas = 0;
for (const fonte of fontes) {
	const saida = fonte.replace(/\.mmd$/, '.png');
	// -w largo para o Mermaid nao encolher o desenho; -s 3 da resolucao de
	// sobra para a impressao.
	const resultado = spawnSync('npx', ['-y', MERMAID_CLI, '-i', `"${fonte}"`, '-o', `"${saida}"`,
		'-c', `"${CONFIG}"`, ...argsPuppeteer.map(arg => `"${arg}"`), '-b', 'white', '-w', '4000', '-s', '3'],
	{ shell: true, encoding: 'utf8', env: { ...process.env, PUPPETEER_SKIP_DOWNLOAD: 'true' } });

	if (resultado.status === 0) {
		console.log(`ok     ${basename(saida)}`);
	} else {
		falhas++;
		console.error(`FALHOU ${basename(fonte)}\n${resultado.stderr}`);
	}
}

process.exit(falhas ? 1 : 0);
