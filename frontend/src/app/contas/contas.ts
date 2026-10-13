import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../core/autenticacao.service';
import { Barra } from '../core/barra/barra';
import { ContaService } from '../core/conta.service';
import { Conta } from '../core/modelos';

/**
 * Contas de acesso (RF12).
 *
 * Criar conta nao esta aqui: contas nascem pelo convite. Esta tela so
 * concede e retira o perfil administrativo -- o que a diretoria chamou
 * de "status da conta, que dependendo libera ou tira recursos".
 */
@Component({
	selector: 'app-contas',
	imports: [Barra],
	templateUrl: './contas.html',
	styleUrl: './contas.css'
})
export class Contas {

	private readonly contas = inject(ContaService);
	private readonly autenticacao = inject(AutenticacaoService);

	readonly lista = signal<Conta[]>([]);
	readonly erro = signal<string | null>(null);
	readonly aviso = signal<string | null>(null);

	/** Login de quem esta usando a tela, para marcar a propria conta. */
	readonly meuLogin = signal<string | null>(null);

	constructor() {
		this.meuLogin.set(this.autenticacao.usuario()?.login ?? null);
		this.carregar();
	}

	private carregar(): void {
		this.contas.listarContas().subscribe({
			next: lista => this.lista.set(lista),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	eAdministrador(conta: Conta): boolean {
		return conta.perfil === 'ADMIN';
	}

	alternarPerfil(conta: Conta): void {
		this.erro.set(null);
		this.aviso.set(null);

		const novo = this.eAdministrador(conta) ? 'PROFISSIONAL' : 'ADMIN';

		this.contas.alterarPerfil(conta.id, novo).subscribe({
			next: atualizada => {
				this.carregar();
				this.aviso.set(atualizada.perfil === 'ADMIN'
					? `${atualizada.login} agora administra o sistema.`
					: `${atualizada.login} deixou de administrar o sistema.`);
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	/**
	 * O 409 aqui tem causa unica e vale explicar: e a guarda que impede
	 * a associacao de ficar sem nenhum administrador. A mensagem do
	 * servidor ja diz o que fazer, entao e ela que aparece.
	 */
	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		return falha.error?.mensagem ?? 'Nao foi possivel completar a operacao.';
	}
}
