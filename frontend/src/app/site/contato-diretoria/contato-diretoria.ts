import { Component } from '@angular/core';
import { CONTATO_DA_DIRETORIA } from '../conteudo';

/**
 * Canais de contato da diretoria, usados no inicio e no associe-se.
 *
 * Mostra so o que estiver preenchido em conteudo.ts. Sem nenhum canal,
 * diz que eles serao publicados -- e nao exibe um contato inventado.
 */
@Component({
	selector: 'app-contato-diretoria',
	templateUrl: './contato-diretoria.html',
	styleUrl: './contato-diretoria.css'
})
export class ContatoDiretoria {

	readonly contato = CONTATO_DA_DIRETORIA;

	readonly algumCanal = Object.values(CONTATO_DA_DIRETORIA).some(valor => !!valor);

	/** Telefone em formato de link: so digitos, com o +55 do Brasil. */
	linkDoTelefone(telefone: string): string {
		const digitos = telefone.replace(/\D/g, '');
		return `tel:+55${digitos.replace(/^55/, '')}`;
	}
}
