import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Barra } from '../core/barra/barra';
import { FichaVinculos } from '../core/ficha-vinculos/ficha-vinculos';
import { ProfissionalService } from '../core/profissional.service';
import { cpfValido, naoFutura } from '../core/validadores';

/**
 * Cadastro e correcao de profissional pela diretoria (RF01).
 *
 * A mesma tela cria e edita: a diferenca e a presenca do id na rota.
 *
 * Areas, formacoes e certificados ficam no FichaVinculos, compartilhado
 * com o autoatendimento do associado -- as secoes sao as mesmas, muda
 * so quem chega nelas. Elas so aparecem na edicao, porque vincular
 * qualquer coisa exige um profissional ja existente.
 */
@Component({
	selector: 'app-cadastro',
	imports: [ReactiveFormsModule, RouterLink, Barra, FichaVinculos],
	templateUrl: './cadastro.html',
	styleUrl: './cadastro.css'
})
export class Cadastro {

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);
	private readonly rota = inject(ActivatedRoute);
	private readonly router = inject(Router);

	readonly id = signal<number | null>(null);
	readonly ativo = signal<boolean | null>(null);
	readonly salvando = signal(false);
	readonly erro = signal<string | null>(null);
	readonly aviso = signal<string | null>(null);

	readonly formulario = this.formBuilder.nonNullable.group({
		nome: ['', [Validators.required, Validators.maxLength(150)]],
		cpf: ['', [Validators.required, cpfValido]],
		dataNascimento: ['', [Validators.required, naoFutura]],
		dataAssociacao: ['', Validators.required]
	});

	constructor() {
		const parametro = this.rota.snapshot.paramMap.get('id');

		if (parametro) {
			const id = Number(parametro);
			this.id.set(id);
			this.carregar(id);
		}
	}

	private carregar(id: number): void {
		this.profissionais.buscarPorId(id).subscribe({
			next: ficha => {
				this.ativo.set(ficha.ativo);
				this.formulario.patchValue({
					nome: ficha.nome,
					cpf: ficha.cpf,
					dataNascimento: ficha.dataNascimento,
					dataAssociacao: ficha.dataAssociacao
				});
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	/**
	 * Tira ou devolve alguem ao quadro de associados.
	 *
	 * Nao existe exclusao administrativa: apagar destruiria o registro
	 * de que a pessoa foi associada. Exclusao definitiva so pelo pedido
	 * do titular, em "Dados pessoais".
	 */
	alterarSituacao(): void {
		const id = this.id();
		const atual = this.ativo();
		if (id === null || atual === null) {
			return;
		}

		this.erro.set(null);
		this.aviso.set(null);

		this.profissionais.alterarSituacao(id, !atual).subscribe({
			next: ficha => {
				this.ativo.set(ficha.ativo);
				this.aviso.set(ficha.ativo
					? 'Profissional reativado: volta a aparecer nas consultas.'
					: 'Profissional desativado: sai das consultas por situacao, '
						+ 'mas o cadastro e o histórico continuam guardados.');
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	salvar(): void {
		if (this.formulario.invalid) {
			this.formulario.markAllAsTouched();
			return;
		}

		this.salvando.set(true);
		this.erro.set(null);
		this.aviso.set(null);

		const dados = this.formulario.getRawValue();
		const id = this.id();

		const operacao = id
			? this.profissionais.atualizar(id, dados)
			: this.profissionais.criar(dados);

		operacao.subscribe({
			next: ficha => {
				this.salvando.set(false);
				if (id) {
					this.aviso.set('Cadastro atualizado.');
				} else {
					// Entra no modo edicao para liberar as secoes, que
					// precisam do profissional ja criado.
					this.id.set(ficha.id);
					this.aviso.set('Profissional cadastrado. '
						+ 'Agora e possivel vincular areas, formacoes e certificados.');
					this.router.navigate(['/profissionais', ficha.id, 'editar'], { replaceUrl: true });
				}
			},
			error: (falha: HttpErrorResponse) => {
				this.salvando.set(false);
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		// O 403 nao ganha mensagem propria: o back-end distingue falta
		// de permissao de falha de token CSRF, e trocar as duas por um
		// texto generico torna o erro indiagnosticavel.
		return falha.error?.mensagem ?? 'Nao foi possivel salvar.';
	}
}
