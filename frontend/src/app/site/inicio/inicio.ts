import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContatoDiretoria } from '../contato-diretoria/contato-diretoria';

/** Pagina inicial do site publico. */
@Component({
	selector: 'app-inicio',
	imports: [RouterLink, ContatoDiretoria],
	templateUrl: './inicio.html',
	styleUrl: './inicio.css'
})
export class Inicio {
}
