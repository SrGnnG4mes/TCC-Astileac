import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../core/autenticacao.service';

@Component({
	selector: 'app-login',
	imports: [ReactiveFormsModule, RouterLink],
	templateUrl: './login.html',
	styleUrl: './login.css'
})
export class Login {

	private readonly formBuilder = inject(FormBuilder);
	private readonly autenticacao = inject(AutenticacaoService);
	private readonly router = inject(Router);

	readonly formulario = this.formBuilder.nonNullable.group({
		login: ['', Validators.required],
		senha: ['', Validators.required]
	});

	readonly enviando = signal(false);
	readonly erro = signal<string | null>(null);

	entrar(): void {
		if (this.formulario.invalid) {
			this.formulario.markAllAsTouched();
			return;
		}

		this.enviando.set(true);
		this.erro.set(null);

		const { login, senha } = this.formulario.getRawValue();

		this.autenticacao.entrar(login, senha).subscribe({
			// A diretoria cai na consulta do quadro; o associado, no
			// painel do proprio cadastro. Espelha a regra do back-end:
			// GET /profissionais e hasRole('ADMIN').
			next: usuario => this.router.navigate(
				[usuario.perfil === 'ADMIN' ? '/profissionais' : '/painel']
			),
			error: (falha: HttpErrorResponse) => {
				this.enviando.set(false);
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	/**
	 * Traduz a falha para algo que ajude quem esta na tela.
	 *
	 * O 401 e credencial errada e tem mensagem propria. O 403 aqui quase
	 * sempre e token CSRF -- o back-end distingue as duas causas do 403
	 * justamente para este caso ser diagnosticavel. O 0 nao e resposta
	 * do servidor: e requisicao que nao chegou, tipicamente back-end
	 * desligado.
	 */
	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor. Verifique se o back-end esta rodando.';
		}
		if (falha.status === 401) {
			return 'Login ou senha incorretos.';
		}
		return falha.error?.mensagem ?? 'Nao foi possivel entrar. Tente novamente.';
	}
}
