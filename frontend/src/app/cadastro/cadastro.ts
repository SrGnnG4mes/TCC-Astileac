import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../core/autenticacao.service';
import { ProfissionalService } from '../core/profissional.service';
import { AreaAtuacao, ProfissionalArea } from '../core/modelos';
import { cpfValido, naoFutura } from '../core/validadores';

/**
 * Cadastro e correcao de profissional (RF01) e vinculo de areas (RF04).
 *
 * A mesma tela cria e edita: a diferenca e a presenca do id na rota. As
 * areas so aparecem na edicao, porque associar uma area exige um
 * profissional ja existente para vincular.
 */
@Component({
	selector: 'app-cadastro',
	imports: [ReactiveFormsModule, RouterLink],
	templateUrl: './cadastro.html',
	styleUrl: './cadastro.css'
})
export class Cadastro {

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);
	private readonly autenticacao = inject(AutenticacaoService);
	private readonly rota = inject(ActivatedRoute);
	private readonly router = inject(Router);

	readonly usuario = this.autenticacao.usuario;

	readonly id = signal<number | null>(null);
	readonly salvando = signal(false);
	readonly erro = signal<string | null>(null);
	readonly aviso = signal<string | null>(null);

	readonly areas = signal<AreaAtuacao[]>([]);
	readonly areasDoProfissional = signal<ProfissionalArea[]>([]);

	readonly formulario = this.formBuilder.nonNullable.group({
		nome: ['', [Validators.required, Validators.maxLength(150)]],
		cpf: ['', [Validators.required, cpfValido]],
		dataNascimento: ['', [Validators.required, naoFutura]],
		dataAssociacao: ['', Validators.required]
	});

	readonly vinculo = this.formBuilder.nonNullable.group({
		areaAtuacaoId: ['', Validators.required],
		dataInicio: ['', [Validators.required, naoFutura]]
	});

	constructor() {
		const parametro = this.rota.snapshot.paramMap.get('id');

		if (parametro) {
			const id = Number(parametro);
			this.id.set(id);
			this.carregar(id);
			this.carregarAreas(id);
		}

		this.profissionais.listarAreas().subscribe({
			next: areas => this.areas.set(areas)
		});
	}

	private carregar(id: number): void {
		this.profissionais.buscarPorId(id).subscribe({
			next: ficha => this.formulario.patchValue({
				nome: ficha.nome,
				cpf: ficha.cpf,
				dataNascimento: ficha.dataNascimento,
				dataAssociacao: ficha.dataAssociacao
			}),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	private carregarAreas(id: number): void {
		this.profissionais.listarAreasDo(id).subscribe({
			next: lista => this.areasDoProfissional.set(lista)
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
					// Entra no modo edicao para liberar o vinculo de
					// areas, que precisa do profissional ja criado.
					this.id.set(ficha.id);
					this.carregarAreas(ficha.id);
					this.aviso.set('Profissional cadastrado. Agora e possivel vincular areas de atuacao.');
					this.router.navigate(['/profissionais', ficha.id, 'editar'], { replaceUrl: true });
				}
			},
			error: (falha: HttpErrorResponse) => {
				this.salvando.set(false);
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	vincularArea(): void {
		const id = this.id();
		if (!id || this.vinculo.invalid) {
			this.vinculo.markAllAsTouched();
			return;
		}

		const { areaAtuacaoId, dataInicio } = this.vinculo.getRawValue();

		this.profissionais.associarArea(id, { areaAtuacaoId: Number(areaAtuacaoId), dataInicio })
			.subscribe({
				next: () => {
					this.vinculo.reset({ areaAtuacaoId: '', dataInicio: '' });
					this.carregarAreas(id);
				},
				error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
			});
	}

	desvincularArea(areaAtuacaoId: number): void {
		const id = this.id();
		if (!id) {
			return;
		}
		this.profissionais.desassociarArea(id, areaAtuacaoId).subscribe({
			next: () => this.carregarAreas(id),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	sair(): void {
		this.autenticacao.sair().subscribe({
			next: () => this.router.navigate(['/login'])
		});
	}

	/**
	 * O 409 tem tratamento proprio porque e o caso mais comum aqui, e a
	 * mensagem do servidor nao repete o CPF -- de proposito: repetir
	 * tornaria a diferenca entre 201 e 409 uma consulta de filiacao.
	 */
	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		if (falha.status === 403) {
			return 'Seu perfil nao permite esta operacao.';
		}
		return falha.error?.mensagem ?? 'Nao foi possivel salvar.';
	}
}
