import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { VALORES_DE_REFERENCIA, VIGENCIA_DOS_VALORES } from '../conteudo';

/**
 * Valores de referencia dos servicos de traducao e interpretacao.
 *
 * Os valores vem de conteudo.ts. Enquanto a diretoria nao os definir, a
 * pagina diz que a tabela sera publicada, em vez de mostrar numeros
 * provisorios que alguem poderia tomar como oficiais.
 */
@Component({
	selector: 'app-valores',
	imports: [RouterLink],
	templateUrl: './valores.html',
	styleUrl: './valores.css'
})
export class Valores {

	grupos = VALORES_DE_REFERENCIA;
	vigencia = VIGENCIA_DOS_VALORES;
}
