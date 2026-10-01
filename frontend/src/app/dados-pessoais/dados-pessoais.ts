import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../core/autenticacao.service';
import { Barra } from '../core/barra/barra';
import { DadosPessoaisService } from '../core/dados-pessoais.service';
import { DadosPessoais, ExclusaoResposta, LogAcesso, Pagina } from '../core/modelos';

/**
 * Direitos do titular (RF10) e registro de acesso (RF11).
 *
 * Fecha o achado 13 da revisao: o back-end ja atendia ao Art. 18 desde
 * a branch feat/direitos-titular-log-acesso, mas nenhuma tela dava
 * acesso a isso. Um direito que so o titular pode exercer nao se
 * cumpre se ele nao alcanca.
 *
 * O back-end libera a ADMIN ou ao proprio titular (@PreAuthorize), e e
 * ele quem decide -- esta tela apenas expoe.
 */
@Component({
	selector: 'app-dados-pessoais',
	imports: [RouterLink, Barra],
	templateUrl: './dados-pessoais.html',
	styleUrl: './dados-pessoais.css'
})
export class DadosPessoaisTela {

	private readonly servico = inject(DadosPessoaisService);
	private readonly autenticacao = inject(AutenticacaoService);
	private readonly rota = inject(ActivatedRoute);
	private readonly router = inject(Router);

	readonly usuario = this.autenticacao.usuario;

	readonly profissionalId = signal<number | null>(null);
	readonly dados = signal<DadosPessoais | null>(null);
	readonly registro = signal<Pagina<LogAcesso> | null>(null);
	readonly exclusao = signal<ExclusaoResposta | null>(null);
	readonly confirmando = signal(false);
	readonly erro = signal<string | null>(null);

	constructor() {
		const parametro = this.rota.snapshot.paramMap.get('id');
		const id = parametro ? Number(parametro) : this.usuario()?.profissionalId ?? null;

		this.profissionalId.set(id);

		if (id === null) {
			// Perfil ADMIN sem profissional vinculado: nao ha "meus
			// dados" porque o login administrativo nao e um associado.
			this.erro.set('Este usuario nao esta vinculado a um cadastro de profissional.');
			return;
		}

		this.carregar(id);
	}

	private carregar(id: number): void {
		this.servico.exportar(id).subscribe({
			next: dados => this.dados.set(dados),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});

		this.servico.registroDeAcesso(id).subscribe({
			next: pagina => this.registro.set(pagina),
			error: () => { /* o registro e complementar; falha nele nao impede ver os dados */ }
		});
	}

	/**
	 * Portabilidade: entrega o conteudo num arquivo que a pessoa leva
	 * consigo. E o mesmo JSON que a API devolve, sem recorte -- recortar
	 * aqui transformaria "todos os seus dados" em "o que a tela achou
	 * conveniente mostrar".
	 */
	baixar(): void {
		const dados = this.dados();
		if (!dados) {
			return;
		}

		const conteudo = JSON.stringify(dados, null, 2);
		const url = URL.createObjectURL(new Blob([conteudo], { type: 'application/json' }));
		const ancora = document.createElement('a');
		ancora.href = url;
		ancora.download = `dados-pessoais-${dados.identificacao.id}.json`;
		ancora.click();
		URL.revokeObjectURL(url);
	}

	pedirConfirmacao(): void {
		this.confirmando.set(true);
	}

	cancelarConfirmacao(): void {
		this.confirmando.set(false);
	}

	solicitarExclusao(): void {
		const id = this.profissionalId();
		if (id === null) {
			return;
		}

		this.confirmando.set(false);
		this.erro.set(null);

		this.servico.solicitarExclusao(id).subscribe({
			next: resposta => {
				this.exclusao.set(resposta);
				this.carregar(id);
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	cancelarExclusao(): void {
		const id = this.profissionalId();
		if (id === null) {
			return;
		}

		this.servico.cancelarExclusao(id).subscribe({
			next: resposta => {
				this.exclusao.set(resposta);
				this.carregar(id);
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		if (falha.status === 403) {
			return 'Voce so pode acessar os proprios dados.';
		}
		return falha.error?.mensagem ?? 'Nao foi possivel completar a operacao.';
	}
}
