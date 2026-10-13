import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ContaService } from '../core/conta.service';
import { cpfValido, naoFutura } from '../core/validadores';

/**
 * Cadastro feito pelo proprio associado, com o convite em maos (RF12).
 *
 * UNICA tela do sistema alcancavel sem estar logado -- e nao poderia ser
 * diferente: quem usa o convite ainda nao tem conta. O que faz as vezes
 * de autenticacao aqui e o proprio token, emitido pela diretoria depois
 * da confirmacao do pagamento, de uso unico e com prazo.
 *
 * O token chega na URL porque e assim que um link de e-mail funciona.
 * Dali em diante ele vai no CORPO das requisicoes, nunca em outra URL:
 * caminho e query string aparecem no log do servidor, em proxy e no
 * Referer.
 */
@Component({
	selector: 'app-cadastro-por-convite',
	imports: [ReactiveFormsModule, RouterLink],
	templateUrl: './cadastro-por-convite.html',
	styleUrl: './cadastro-por-convite.css'
})
export class CadastroPorConvite {

	private readonly formBuilder = inject(FormBuilder);
	private readonly contas = inject(ContaService);
	private readonly rota = inject(ActivatedRoute);
	private readonly router = inject(Router);

	readonly token = signal<string | null>(null);
	readonly emailConvidado = signal<string | null>(null);
	readonly conferindo = signal(true);
	readonly motivoInvalido = signal<string | null>(null);
	readonly enviando = signal(false);
	readonly erro = signal<string | null>(null);
	readonly concluido = signal(false);

	readonly formulario = this.formBuilder.nonNullable.group({
		nome: ['', [Validators.required, Validators.maxLength(150)]],
		cpf: ['', [Validators.required, cpfValido]],
		dataNascimento: ['', [Validators.required, naoFutura]],
		login: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(100)]],
		senha: ['', [Validators.required, Validators.minLength(8)]]
	});

	constructor() {
		const token = this.rota.snapshot.queryParamMap.get('convite');

		if (!token) {
			this.conferindo.set(false);
			this.motivoInvalido.set(
				'Este endereco precisa do codigo do convite. Use o link que a associacao enviou.');
			return;
		}

		this.token.set(token);

		this.contas.validarConvite(token).subscribe({
			next: resultado => {
				this.conferindo.set(false);
				if (resultado.valido) {
					this.emailConvidado.set(resultado.email);
				} else {
					this.motivoInvalido.set(resultado.motivo);
				}
			},
			error: (falha: HttpErrorResponse) => {
				this.conferindo.set(false);
				this.motivoInvalido.set(this.mensagemDe(falha));
			}
		});
	}

	cadastrar(): void {
		const token = this.token();
		if (!token || this.formulario.invalid) {
			this.formulario.markAllAsTouched();
			return;
		}

		this.enviando.set(true);
		this.erro.set(null);

		this.contas.cadastrarPorConvite({ token, ...this.formulario.getRawValue() }).subscribe({
			next: () => {
				this.enviando.set(false);
				this.concluido.set(true);
			},
			error: (falha: HttpErrorResponse) => {
				this.enviando.set(false);
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	irParaLogin(): void {
		this.router.navigate(['/login']);
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		return falha.error?.mensagem ?? 'Nao foi possivel concluir o cadastro.';
	}
}
