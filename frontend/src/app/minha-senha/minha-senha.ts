import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Barra } from '../core/barra/barra';
import { ContaService } from '../core/conta.service';

/** A confirmacao tem de repetir a senha nova. */
function senhasIguais(grupo: AbstractControl): ValidationErrors | null {
	const nova = grupo.get('senhaNova')?.value;
	const confirmacao = grupo.get('confirmacao')?.value;
	return nova && confirmacao && nova !== confirmacao ? { diferentes: true } : null;
}

/**
 * Troca da propria senha (RF12), para qualquer conta.
 *
 * Antes ficava no fim do "Meu cadastro", que so existe para quem tem
 * cadastro de associado: uma conta so da diretoria -- como a criada na
 * instalacao -- nao tinha onde trocar a senha. Agora o link fica na
 * barra, para todo mundo.
 *
 * Exige a senha atual: sem ela, uma sessao esquecida aberta bastaria
 * para trocar a senha e trancar o dono fora da propria conta. E pede a
 * senha nova duas vezes, porque um erro de digitacao aqui faria o mesmo.
 */
@Component({
	selector: 'app-minha-senha',
	imports: [ReactiveFormsModule, Barra],
	templateUrl: './minha-senha.html',
	styleUrl: './minha-senha.css'
})
export class MinhaSenha {

	private readonly formBuilder = inject(FormBuilder);
	private readonly contas = inject(ContaService);

	readonly formulario = this.formBuilder.nonNullable.group({
		senhaAtual: ['', Validators.required],
		senhaNova: ['', [Validators.required, Validators.minLength(8)]],
		confirmacao: ['', Validators.required]
	}, { validators: senhasIguais });

	readonly enviando = signal(false);
	readonly erro = signal<string | null>(null);
	readonly aviso = signal<string | null>(null);

	trocar(): void {
		if (this.formulario.invalid) {
			this.formulario.markAllAsTouched();
			return;
		}

		this.enviando.set(true);
		this.erro.set(null);
		this.aviso.set(null);

		const { senhaAtual, senhaNova } = this.formulario.getRawValue();

		this.contas.trocarPropriaSenha(senhaAtual, senhaNova).subscribe({
			next: () => {
				this.enviando.set(false);
				this.formulario.reset({ senhaAtual: '', senhaNova: '', confirmacao: '' });
				this.aviso.set('Senha alterada. Use a senha nova da próxima vez que entrar.');
			},
			error: (falha: HttpErrorResponse) => {
				this.enviando.set(false);
				this.erro.set(falha.status === 0
					? 'Não foi possível falar com o servidor.'
					: falha.error?.mensagem ?? 'Não foi possível alterar a senha.');
			}
		});
	}
}
