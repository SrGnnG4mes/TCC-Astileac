import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AutenticacaoService } from '../core/autenticacao.service';

/**
 * Tela minima pos-login.
 *
 * Existe para fechar a primeira fatia vertical: ela faz uma consulta
 * AUTENTICADA (/autenticacao/eu), o que exercita de ponta a ponta o
 * caminho que concentra o risco -- proxy do servidor de
 * desenvolvimento, cookie de sessao, CSRF e login por formulario.
 *
 * As telas de consulta do RF08 entram em cima disto, depois que este
 * caminho estiver verificado.
 */
@Component({
	selector: 'app-painel',
	templateUrl: './painel.html',
	styleUrl: './painel.css'
})
export class Painel {

	private readonly autenticacao = inject(AutenticacaoService);
	private readonly router = inject(Router);

	readonly usuario = this.autenticacao.usuario;
	readonly erro = signal<string | null>(null);

	constructor() {
		// Recarregar a pagina perde o estado local, mas nao a sessao: o
		// cookie continua valido, entao basta perguntar ao servidor de
		// novo. Se ele responder 401, a sessao acabou de verdade.
		if (!this.usuario()) {
			this.autenticacao.carregarUsuario().subscribe({
				error: () => this.router.navigate(['/login'])
			});
		}
	}

	sair(): void {
		this.autenticacao.sair().subscribe({
			next: () => this.router.navigate(['/login']),
			error: () => this.erro.set('Nao foi possivel encerrar a sessao.')
		});
	}
}
