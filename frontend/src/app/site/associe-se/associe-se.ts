import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContatoDiretoria } from '../contato-diretoria/contato-diretoria';

/**
 * Como se associar.
 *
 * Descreve o fluxo que o sistema ja implementa (RF12): a pessoa fala com
 * a diretoria e paga; a diretoria confirma e emite um convite; o convite
 * chega por e-mail, vale por 7 dias e serve uma vez so; a propria pessoa
 * faz o cadastro pelo link. Nao ha formulario de inscricao aqui de
 * proposito: o pagamento ainda e confirmado fora do sistema, e o convite
 * e o que liga o cadastro a quem a associacao reconheceu como associado.
 */
@Component({
	selector: 'app-associe-se',
	imports: [RouterLink, ContatoDiretoria],
	templateUrl: './associe-se.html',
	styleUrl: './associe-se.css'
})
export class AssocieSe {
}
