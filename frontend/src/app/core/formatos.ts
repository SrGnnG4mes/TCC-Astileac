import { Pipe, PipeTransform } from '@angular/core';
import { AcaoLog, Contato, Perfil } from './modelos';

/**
 * Data no formato brasileiro: "2026-10-20" vira "20/10/2026", e
 * "2026-10-20T20:00:24.409" vira "20/10/2026 às 20:00".
 *
 * Le o texto direto, sem passar por new Date(). A API manda datas sem
 * fuso, e new Date("2020-01-01") e meia-noite em UTC -- no fuso do Acre
 * (UTC-5) isso apareceria como 31/12/2019. Formatar a partir do texto
 * mostra exatamente o dia que o servidor gravou.
 */
@Pipe({ name: 'dataBr' })
export class DataBrPipe implements PipeTransform {

	transform(valor: string | null | undefined): string {
		if (!valor) {
			return '';
		}
		const partes = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/.exec(valor);
		if (!partes) {
			return valor;
		}
		const [, ano, mes, dia, hora, minuto] = partes;
		const data = `${dia}/${mes}/${ano}`;
		return hora ? `${data} às ${hora}:${minuto}` : data;
	}
}

const ACOES: Record<AcaoLog, string> = {
	CONSULTA: 'Consulta',
	CRIACAO: 'Criação',
	ALTERACAO: 'Alteração',
	EXCLUSAO: 'Exclusão',
	EXPORTACAO: 'Exportação'
};

/**
 * Nome legivel da acao do registro de acesso (RF11). O enum do back-end
 * nao tem acento por ser identificador; a tela mostra a palavra.
 */
@Pipe({ name: 'acao' })
export class AcaoPipe implements PipeTransform {

	transform(acao: AcaoLog): string {
		return ACOES[acao] ?? acao;
	}
}

const PERFIS: Record<Perfil, string> = {
	ADMIN: 'diretoria',
	PROFISSIONAL: 'associado'
};

/** O perfil como a associacao fala dele: "diretoria" ou "associado". */
@Pipe({ name: 'perfil' })
export class PerfilPipe implements PipeTransform {

	transform(perfil: Perfil): string {
		return PERFIS[perfil] ?? perfil;
	}
}

const ENTIDADES: Record<string, string> = {
	PROFISSIONAIS: 'Cadastro do profissional',
	'AREAS-ATUACAO': 'Áreas de atuação'
};

/**
 * O que foi acessado, no registro de acesso. O back-end grava o primeiro
 * trecho da URL ("PROFISSIONAIS"); a tela mostra o nome do registro.
 */
@Pipe({ name: 'entidade' })
export class EntidadePipe implements PipeTransform {

	transform(entidade: string): string {
		return ENTIDADES[entidade] ?? entidade;
	}
}

/**
 * Valor do contato para exibicao. O telefone e guardado so com digitos
 * (o back-end exige DDD + 8 ou 9): "68999998888" vira "(68) 99999-8888".
 * E-mail passa como esta.
 *
 * Fica aqui, e nao numa tela, porque duas telas mostram contatos: a ficha
 * completa e a ficha rapida da consulta.
 */
@Pipe({ name: 'contato' })
export class ContatoPipe implements PipeTransform {

	transform(contato: Contato): string {
		const v = contato.valor;
		if (contato.tipo !== 'TELEFONE') {
			return v;
		}
		if (v.length === 11) {
			return `(${v.slice(0, 2)}) ${v.slice(2, 7)}-${v.slice(7)}`;
		}
		if (v.length === 10) {
			return `(${v.slice(0, 2)}) ${v.slice(2, 6)}-${v.slice(6)}`;
		}
		return v;
	}
}
