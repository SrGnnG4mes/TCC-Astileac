import { Injectable, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * As duas areas do sistema: a da diretoria, que administra o quadro, e a
 * do associado, que cuida do proprio cadastro.
 *
 * Um membro da diretoria tambem e associado, entao a mesma conta alcanca
 * as duas. O que nao pode e uma tela misturar as duas: a ficha mostrar o
 * botao de visto da diretoria no cadastro do proprio diretor, ou a tela
 * de dados pessoais falar "meus dados" quando a diretoria abre os dados
 * de outra pessoa. Por isso cada rota declara a sua area (data.area) e e
 * a AREA, e nao o perfil da conta, que decide o que a tela mostra.
 */
export type Area = 'diretoria' | 'associado';

/**
 * Area declarada na rota da tela. Nulo nas telas da propria conta, como
 * a troca de senha, que valem igual nas duas areas.
 */
export function areaDaRota(rota: ActivatedRoute): Area | null {
	const area = rota.snapshot.data['area'];
	return area === 'diretoria' || area === 'associado' ? area : null;
}

/**
 * Ultima area por onde a pessoa passou. A troca de senha nao e de nenhuma
 * area; ali a barra continua na area de onde a pessoa veio, em vez de
 * pular para a outra.
 */
@Injectable({ providedIn: 'root' })
export class UltimaArea {
	readonly valor = signal<Area | null>(null);
}
