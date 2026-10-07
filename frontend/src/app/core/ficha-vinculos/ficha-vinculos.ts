import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../autenticacao.service';
import { ProfissionalService } from '../profissional.service';
import { AreaAtuacao, Certificado, Formacao, ProfissionalArea } from '../modelos';
import { naoFutura } from '../validadores';

/**
 * Areas de atuacao, formacoes e certificados de um profissional
 * (RF04, RF05, RF06).
 *
 * Componente compartilhado porque as mesmas tres secoes aparecem em
 * duas telas com donos diferentes: a ficha que a diretoria administra
 * e o autoatendimento do proprio associado. O back-end ja autoriza os
 * dois -- os controllers sao
 * `hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#profissionalId)`
 * --, entao nao ha nada a distinguir aqui: quem pode o que e decidido
 * no servidor, e este componente apenas chama a API.
 *
 * As tres secoes ficam juntas, e nao em tres componentes, porque
 * compartilham estado: o seletor de formacao do certificado depende da
 * lista de formacoes, que muda quando uma formacao e adicionada ou
 * removida. Separa-las exigiria coordenacao entre irmaos para ganhar
 * nada.
 */
@Component({
	selector: 'app-ficha-vinculos',
	imports: [ReactiveFormsModule],
	templateUrl: './ficha-vinculos.html',
	styleUrl: './ficha-vinculos.css'
})
export class FichaVinculos implements OnInit {

	/** Dono dos registros. Vem da rota (diretoria) ou da sessao (associado). */
	readonly profissionalId = input.required<number>();

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);
	private readonly autenticacao = inject(AutenticacaoService);

	readonly areas = signal<AreaAtuacao[]>([]);
	readonly areasDoProfissional = signal<ProfissionalArea[]>([]);
	readonly formacoes = signal<Formacao[]>([]);
	readonly certificados = signal<Certificado[]>([]);
	readonly erro = signal<string | null>(null);
	/** Id do certificado cujo upload esta em curso, para desabilitar o botao. */
	readonly enviando = signal<number | null>(null);

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

	ngOnInit(): void {
		this.profissionais.listarAreas().subscribe({
			next: areas => this.areas.set(areas)
		});
		this.recarregar();
	}

	private recarregar(): void {
		const id = this.profissionalId();
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

	// ---------- areas (RF04) ----------

	vincularArea(): void {
		if (this.vinculo.invalid) {
			this.vinculo.markAllAsTouched();
			return;
		}

		const { areaAtuacaoId, dataInicio } = this.vinculo.getRawValue();

		this.profissionais
			.associarArea(this.profissionalId(), { areaAtuacaoId: Number(areaAtuacaoId), dataInicio })
			.subscribe({
				next: () => {
					this.vinculo.reset({ areaAtuacaoId: '', dataInicio: '' });
					this.recarregar();
				},
				error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
			});
	}

	desvincularArea(areaAtuacaoId: number): void {
		this.profissionais.desassociarArea(this.profissionalId(), areaAtuacaoId).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	// ---------- formacoes (RF05) ----------

	adicionarFormacao(): void {
		if (this.novaFormacao.invalid) {
			this.novaFormacao.markAllAsTouched();
			return;
		}

		const dados = this.novaFormacao.getRawValue();

		this.profissionais.criarFormacao(this.profissionalId(), {
			nivel: dados.nivel.trim(),
			curso: dados.curso.trim(),
			instituicao: dados.instituicao.trim(),
			anoConclusao: Number(dados.anoConclusao)
		}).subscribe({
			next: () => {
				this.novaFormacao.reset({ nivel: '', curso: '', instituicao: '', anoConclusao: '' });
				this.recarregar();
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	removerFormacao(formacaoId: number): void {
		this.profissionais.removerFormacao(this.profissionalId(), formacaoId).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	// ---------- certificados (RF06) ----------

	adicionarCertificado(): void {
		if (this.novoCertificado.invalid) {
			this.novoCertificado.markAllAsTouched();
			return;
		}

		const dados = this.novoCertificado.getRawValue();

		this.profissionais.criarCertificado(this.profissionalId(), {
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
				this.recarregar();
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	removerCertificado(certificadoId: number): void {
		this.profissionais.removerCertificado(this.profissionalId(), certificadoId).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	// ---------- documento e reconhecimento (Passo 8 / RF13) ----------

	/** Só a diretoria reconhece; o titular anexa e baixa o próprio. */
	readonly eAdmin = computed(() => this.autenticacao.usuario()?.perfil === 'ADMIN');

	urlDoDocumento(certificadoId: number): string {
		return this.profissionais.urlDoDocumento(this.profissionalId(), certificadoId);
	}

	/**
	 * Estado do certificado, derivado -- nao ha campo de situacao no
	 * back-end, e nao deveria haver: seria estado duplicado, podendo
	 * divergir de `temDocumento` e `validadoEm`.
	 */
	situacao(certificado: Certificado): 'declarado' | 'enviado' | 'reconhecido' {
		if (!certificado.temDocumento) {
			return 'declarado';
		}
		return certificado.validadoEm ? 'reconhecido' : 'enviado';
	}

	anexarDocumento(certificadoId: number, evento: Event): void {
		const entrada = evento.target as HTMLInputElement;
		const arquivo = entrada.files?.[0];
		if (!arquivo) {
			return;
		}

		this.erro.set(null);
		this.enviando.set(certificadoId);

		this.profissionais.anexarDocumento(this.profissionalId(), certificadoId, arquivo).subscribe({
			next: () => {
				this.enviando.set(null);
				// Limpa a selecao: sem isso, escolher o MESMO arquivo de
				// novo nao dispara o evento change e parece que o botao
				// quebrou.
				entrada.value = '';
				this.recarregar();
			},
			error: (falha: HttpErrorResponse) => {
				this.enviando.set(null);
				entrada.value = '';
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	reconhecer(certificadoId: number): void {
		this.erro.set(null);
		this.profissionais.validarCertificado(this.profissionalId(), certificadoId).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	desfazerReconhecimento(certificadoId: number): void {
		this.erro.set(null);
		this.profissionais.invalidarCertificado(this.profissionalId(), certificadoId).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		// O 403 nao ganha mensagem propria: o back-end distingue falta
		// de permissao de falha de token CSRF, e trocar as duas por um
		// texto generico torna o erro indiagnosticavel.
		return falha.error?.mensagem ?? 'Nao foi possivel completar a operacao.';
	}
}
