import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Barra } from '../core/barra/barra';
import { ContaService } from '../core/conta.service';
import { Convite, ConviteEmitido } from '../core/modelos';

/**
 * Emissao e acompanhamento de convites (RF12).
 *
 * A diretoria emite depois de confirmar o pagamento da associacao. E o
 * substituto do gatilho enquanto a integracao com o banco nao existe --
 * quando ela chegar, chamara o MESMO ponto de emissao. A tela continua
 * util de todo modo, para a excecao: quem pagou em dinheiro, e-mail que
 * voltou, associado fundador que nunca passou pelo sistema.
 */
@Component({
	selector: 'app-convites',
	imports: [ReactiveFormsModule, Barra],
	templateUrl: './convites.html',
	styleUrl: './convites.css'
})
export class Convites {

	private readonly formBuilder = inject(FormBuilder);
	private readonly contas = inject(ContaService);

	readonly convites = signal<Convite[]>([]);
	/** O convite recem-emitido, com o token em texto. Some ao emitir outro. */
	readonly emitido = signal<ConviteEmitido | null>(null);
	readonly copiado = signal(false);
	readonly emitindo = signal(false);
	readonly erro = signal<string | null>(null);

	readonly formulario = this.formBuilder.nonNullable.group({
		email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]]
	});

	constructor() {
		this.carregar();
	}

	private carregar(): void {
		this.contas.listarConvites().subscribe({
			next: lista => this.convites.set(lista),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	emitir(): void {
		if (this.formulario.invalid) {
			this.formulario.markAllAsTouched();
			return;
		}

		this.emitindo.set(true);
		this.erro.set(null);
		this.emitido.set(null);
		this.copiado.set(false);

		this.contas.emitirConvite(this.formulario.getRawValue().email).subscribe({
			next: convite => {
				this.emitindo.set(false);
				this.emitido.set(convite);
				this.formulario.reset({ email: '' });
				this.carregar();
			},
			error: (falha: HttpErrorResponse) => {
				this.emitindo.set(false);
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	/** Endereco que a pessoa convidada abre. */
	linkDoConvite(token: string): string {
		return `${location.origin}/cadastro?convite=${encodeURIComponent(token)}`;
	}

	copiarLink(token: string): void {
		navigator.clipboard.writeText(this.linkDoConvite(token)).then(
			() => this.copiado.set(true),
			// Area de transferencia pode estar bloqueada; o link continua
			// visivel na tela para copiar a mao.
			() => this.copiado.set(false)
		);
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		return falha.error?.mensagem ?? 'Nao foi possivel completar a operacao.';
	}
}
