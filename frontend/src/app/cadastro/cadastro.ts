import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Barra } from '../core/barra/barra';
import { ProfissionalService } from '../core/profissional.service';
import { AreaAtuacao, Certificado, Formacao, ProfissionalArea } from '../core/modelos';
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
	imports: [ReactiveFormsModule, RouterLink, Barra],
	templateUrl: './cadastro.html',
	styleUrl: './cadastro.css'
})
export class Cadastro {

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);
	private readonly rota = inject(ActivatedRoute);
	private readonly router = inject(Router);

	readonly id = signal<number | null>(null);
	readonly salvando = signal(false);
	readonly erro = signal<string | null>(null);
	readonly aviso = signal<string | null>(null);

	readonly areas = signal<AreaAtuacao[]>([]);
	readonly areasDoProfissional = signal<ProfissionalArea[]>([]);
	readonly formacoes = signal<Formacao[]>([]);
	readonly certificados = signal<Certificado[]>([]);

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

	readonly novaFormacao = this.formBuilder.nonNullable.group({
		nivel: ['', [Validators.required, Validators.maxLength(50)]],
		curso: ['', [Validators.required, Validators.maxLength(150)]],
		instituicao: ['', [Validators.required, Validators.maxLength(150)]],
		anoConclusao: ['', Validators.required]
	});

	readonly novoCertificado = this.formBuilder.nonNullable.group({
		// Vazio = certificado solto, sem formacao associada. E valido:
		// nem todo certificado decorre de um curso registrado aqui.
		formacaoId: '',
		titulo: ['', [Validators.required, Validators.maxLength(150)]],
		instituicaoEmissora: ['', [Validators.required, Validators.maxLength(150)]],
		cargaHoraria: ['', Validators.required],
		dataEmissao: ['', [Validators.required, naoFutura]]
	});

	constructor() {
		const parametro = this.rota.snapshot.paramMap.get('id');

		if (parametro) {
			const id = Number(parametro);
			this.id.set(id);
			this.carregar(id);
			this.carregarVinculados(id);
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

	/** Recarrega os tres cadastros que pendem do profissional. */
	private carregarVinculados(id: number): void {
		this.profissionais.listarAreasDo(id).subscribe({
			next: lista => this.areasDoProfissional.set(lista)
		});
		this.profissionais.listarFormacoes(id).subscribe({
			next: lista => this.formacoes.set(lista)
		});
		this.profissionais.listarCertificados(id).subscribe({
			next: lista => this.certificados.set(lista)
		});
	}

	/** Nome da formacao para exibir junto ao certificado que a referencia. */
	nomeDaFormacao(formacaoId: number | null): string | null {
		if (!formacaoId) {
			return null;
		}
		return this.formacoes().find(f => f.id === formacaoId)?.curso ?? null;
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
					this.carregarVinculados(ficha.id);
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
					this.carregarVinculados(id);
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
			next: () => this.carregarVinculados(id),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	// ---------- formacoes (RF05) ----------

	adicionarFormacao(): void {
		const id = this.id();
		if (!id || this.novaFormacao.invalid) {
			this.novaFormacao.markAllAsTouched();
			return;
		}

		const dados = this.novaFormacao.getRawValue();

		this.profissionais.criarFormacao(id, {
			nivel: dados.nivel.trim(),
			curso: dados.curso.trim(),
			instituicao: dados.instituicao.trim(),
			anoConclusao: Number(dados.anoConclusao)
		}).subscribe({
			next: () => {
				this.novaFormacao.reset({ nivel: '', curso: '', instituicao: '', anoConclusao: '' });
				this.carregarVinculados(id);
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	removerFormacao(formacaoId: number): void {
		const id = this.id();
		if (!id) {
			return;
		}
		this.profissionais.removerFormacao(id, formacaoId).subscribe({
			next: () => this.carregarVinculados(id),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	// ---------- certificados (RF06) ----------

	adicionarCertificado(): void {
		const id = this.id();
		if (!id || this.novoCertificado.invalid) {
			this.novoCertificado.markAllAsTouched();
			return;
		}

		const dados = this.novoCertificado.getRawValue();

		this.profissionais.criarCertificado(id, {
			// Vazio vira nulo, nao zero: o back-end so aceita nulo ou um
			// id de formacao do proprio profissional.
			formacaoId: dados.formacaoId ? Number(dados.formacaoId) : null,
			titulo: dados.titulo.trim(),
			instituicaoEmissora: dados.instituicaoEmissora.trim(),
			cargaHoraria: Number(dados.cargaHoraria),
			dataEmissao: dados.dataEmissao
		}).subscribe({
			next: () => {
				this.novoCertificado.reset({
					formacaoId: '', titulo: '', instituicaoEmissora: '',
					cargaHoraria: '', dataEmissao: ''
				});
				this.carregarVinculados(id);
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	removerCertificado(certificadoId: number): void {
		const id = this.id();
		if (!id) {
			return;
		}
		this.profissionais.removerCertificado(id, certificadoId).subscribe({
			next: () => this.carregarVinculados(id),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
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
		// O 403 nao ganha mensagem propria: o back-end distingue falta
		// de permissao de falha de token CSRF, e trocar as duas por um
		// texto generico torna o erro indiagnosticavel.
		return falha.error?.mensagem ?? 'Nao foi possivel salvar.';
	}
}
