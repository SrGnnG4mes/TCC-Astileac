import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../autenticacao.service';
import { ProfissionalService } from '../profissional.service';
import {
	AreaAtuacao,
	Certificado,
	Contato,
	Endereco,
	Formacao,
	ProfissionalArea,
	TipoContato,
	Vinculo
} from '../modelos';
import { naoFutura, DATA_MINIMA, hoje } from '../validadores';
import {
	CepPipe, ContatoPipe, DataBrPipe, SeloCertificadoPipe, SituacaoDoCertificado, situacaoDoCertificado
} from '../formatos';

/**
 * Tudo que pende de um profissional: contatos, endereco, areas de
 * atuacao, formacoes, certificados e vinculos empregaticios
 * (RF02 a RF07).
 *
 * Componente compartilhado porque as mesmas secoes aparecem em duas
 * telas com donos diferentes: a ficha que a diretoria administra e o
 * autoatendimento do proprio associado. O back-end ja autoriza os dois
 * -- os controllers sao
 * `hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#profissionalId)`
 * --, entao nao ha nada a distinguir aqui: quem pode o que e decidido
 * no servidor, e este componente apenas chama a API.
 *
 * As secoes ficam num componente so, e nao em seis, porque parte delas
 * compartilha estado: o seletor de formacao do certificado depende da
 * lista de formacoes, que muda quando uma formacao e adicionada ou
 * removida. Separa-las exigiria coordenacao entre irmaos para ganhar
 * nada.
 *
 * A ordem na tela segue a leitura de uma ficha: quem e e como achar
 * (contatos, endereco), o que faz (areas), o que comprova (formacoes,
 * certificados) e onde trabalhou (vinculos).
 */
@Component({
	selector: 'app-ficha-vinculos',
	imports: [ReactiveFormsModule, CepPipe, ContatoPipe, DataBrPipe, SeloCertificadoPipe],
	templateUrl: './ficha-vinculos.html',
	styleUrl: './ficha-vinculos.css'
})
export class FichaVinculos implements OnInit {

	/**
	 * Limites dos campos de data. Com o max, o navegador deixa o ano com 4
	 * digitos e nao oferece data futura no calendario.
	 */
	readonly dataMinima = DATA_MINIMA;
	readonly hoje = hoje();

	/** Dono dos registros. Vem da rota (diretoria) ou da sessao (associado). */
	readonly profissionalId = input.required<number>();

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);
	private readonly autenticacao = inject(AutenticacaoService);

	readonly areas = signal<AreaAtuacao[]>([]);
	readonly areasDoProfissional = signal<ProfissionalArea[]>([]);
	readonly formacoes = signal<Formacao[]>([]);
	readonly certificados = signal<Certificado[]>([]);
	readonly contatos = signal<Contato[]>([]);
	readonly vinculos = signal<Vinculo[]>([]);

	/**
	 * Endereco: `null` significa "ainda nao informado", que e estado
	 * normal -- a relacao e 1:1 opcional. O back-end responde 404 nesse
	 * caso, e o 404 aqui NAO e erro.
	 */
	readonly endereco = signal<Endereco | null>(null);
	/** Formulario de endereco aberto (para criar ou editar). */
	readonly editandoEndereco = signal(false);

	readonly erro = signal<string | null>(null);
	/** Id do certificado cujo upload esta em curso, para desabilitar o botao. */
	readonly enviando = signal<number | null>(null);

	readonly novaArea = this.formBuilder.nonNullable.group({
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

	/**
	 * CEP e UF conferidos ja aqui.
	 *
	 * O CEP aceita COM ou SEM hifen ("69900-000" ou "69900000"), e o hifen
	 * e removido ao salvar, porque o back-end so aceita digitos. A
	 * primeira versao exigia 8 digitos ja no formulario -- e como o
	 * validador roda antes do salvar, quem digitasse o CEP do jeito
	 * normal, com hifen, nao conseguia salvar nunca. Descoberto
	 * preenchendo a tela como uma pessoa preencheria.
	 */
	readonly formEndereco = this.formBuilder.nonNullable.group({
		cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
		logradouro: ['', [Validators.required, Validators.maxLength(150)]],
		numero: ['', [Validators.required, Validators.maxLength(20)]],
		complemento: ['', Validators.maxLength(100)],
		bairro: ['', [Validators.required, Validators.maxLength(100)]],
		cidade: ['', [Validators.required, Validators.maxLength(100)]],
		// Aceita minuscula pelo mesmo motivo do CEP: e convertida ao
		// salvar, e exigir maiuscula aqui travaria quem digita "ac".
		uf: ['AC', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]]
	});

	readonly novoContato = this.formBuilder.nonNullable.group({
		tipo: ['EMAIL' as TipoContato, Validators.required],
		valor: ['', [Validators.required, Validators.maxLength(150)]],
		principal: false
	});

	/**
	 * Vinculo empregaticio. `dataFim` vazia = vinculo em andamento; o
	 * servidor deriva `atual` disso, entao o formulario nao pergunta.
	 */
	readonly novoVinculo = this.formBuilder.nonNullable.group({
		instituicao: ['', [Validators.required, Validators.maxLength(150)]],
		cargo: ['', [Validators.required, Validators.maxLength(100)]],
		tipoVinculo: ['', [Validators.required, Validators.maxLength(50)]],
		dataInicio: ['', [Validators.required, naoFutura]],
		dataFim: ['', naoFutura]
	});

	ngOnInit(): void {
		this.profissionais.listarAreas().subscribe({
			next: areas => this.areas.set(areas)
		});
		this.recarregar();
	}

	private recarregar(): void {
		const id = this.profissionalId();
		this.profissionais.buscarEndereco(id).subscribe({
			next: endereco => this.endereco.set(endereco),
			// 404 aqui e "ainda nao informado", estado normal da relacao
			// 1:1 opcional. Qualquer outro status e erro de verdade.
			error: (falha: HttpErrorResponse) => {
				this.endereco.set(null);
				if (falha.status !== 404) {
					this.erro.set(this.mensagemDe(falha));
				}
			}
		});
		this.profissionais.listarContatos(id).subscribe({
			next: lista => this.contatos.set(lista)
		});
		this.profissionais.listarVinculos(id).subscribe({
			next: lista => this.vinculos.set(lista)
		});
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

	// ---------- endereco (RF02) ----------

	abrirEndereco(): void {
		const atual = this.endereco();
		this.formEndereco.reset({
			cep: atual?.cep ?? '',
			logradouro: atual?.logradouro ?? '',
			numero: atual?.numero ?? '',
			complemento: atual?.complemento ?? '',
			bairro: atual?.bairro ?? '',
			cidade: atual?.cidade ?? '',
			uf: atual?.uf ?? 'AC'
		});
		this.editandoEndereco.set(true);
	}

	cancelarEndereco(): void {
		this.editandoEndereco.set(false);
	}

	/**
	 * Cria ou substitui -- e a mesma chamada nos dois casos, porque o
	 * back-end publica PUT numa rota no singular: cabe um endereco so.
	 */
	salvarEndereco(): void {
		if (this.formEndereco.invalid) {
			this.formEndereco.markAllAsTouched();
			return;
		}

		const dados = this.formEndereco.getRawValue();

		this.profissionais.salvarEndereco(this.profissionalId(), {
			...dados,
			// Mascara comum de CEP ("69900-000") vira so digitos, que e o
			// formato que o back-end aceita.
			cep: dados.cep.replace(/\D/g, ''),
			uf: dados.uf.toUpperCase()
		}).subscribe({
			next: endereco => {
				this.endereco.set(endereco);
				this.editandoEndereco.set(false);
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	removerEndereco(): void {
		this.profissionais.removerEndereco(this.profissionalId()).subscribe({
			next: () => this.endereco.set(null),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	/** CEP para exibicao: "69900000" vira "69900-000". */
	// ---------- contatos (RF03) ----------

	adicionarContato(): void {
		if (this.novoContato.invalid) {
			this.novoContato.markAllAsTouched();
			return;
		}

		const dados = this.novoContato.getRawValue();

		this.profissionais.criarContato(this.profissionalId(), {
			tipo: dados.tipo,
			// Telefone so com digitos: "(68) 99999-8888" vira
			// "68999998888", formato que o back-end valida.
			valor: dados.tipo === 'TELEFONE' ? dados.valor.replace(/\D/g, '') : dados.valor.trim(),
			principal: dados.principal
		}).subscribe({
			next: () => {
				this.novoContato.reset({ tipo: dados.tipo, valor: '', principal: false });
				this.recarregar();
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	/**
	 * Torna principal. O servidor desmarca sozinho o principal anterior
	 * do MESMO tipo -- um e-mail e um telefone principais, nunca dois do
	 * mesmo tipo --, entao basta recarregar.
	 */
	tornarPrincipal(contato: Contato): void {
		this.profissionais.atualizarContato(this.profissionalId(), contato.id, {
			tipo: contato.tipo,
			valor: contato.valor,
			principal: true
		}).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	removerContato(contatoId: number): void {
		this.profissionais.removerContato(this.profissionalId(), contatoId).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	// ---------- vinculos empregaticios (RF07) ----------

	adicionarVinculo(): void {
		if (this.novoVinculo.invalid) {
			this.novoVinculo.markAllAsTouched();
			return;
		}

		const dados = this.novoVinculo.getRawValue();

		this.profissionais.criarVinculo(this.profissionalId(), {
			instituicao: dados.instituicao.trim(),
			cargo: dados.cargo.trim(),
			tipoVinculo: dados.tipoVinculo.trim(),
			dataInicio: dados.dataInicio,
			// Vazio vira nulo, que e como o servidor entende "em andamento".
			dataFim: dados.dataFim || null
		}).subscribe({
			next: () => {
				this.novoVinculo.reset({
					instituicao: '', cargo: '', tipoVinculo: '', dataInicio: '', dataFim: ''
				});
				this.recarregar();
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	removerVinculo(vinculoId: number): void {
		this.profissionais.removerVinculo(this.profissionalId(), vinculoId).subscribe({
			next: () => this.recarregar(),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	// ---------- areas (RF04) ----------

	vincularArea(): void {
		if (this.novaArea.invalid) {
			this.novaArea.markAllAsTouched();
			return;
		}

		const { areaAtuacaoId, dataInicio } = this.novaArea.getRawValue();

		this.profissionais
			.associarArea(this.profissionalId(), { areaAtuacaoId: Number(areaAtuacaoId), dataInicio })
			.subscribe({
				next: () => {
					this.novaArea.reset({ areaAtuacaoId: '', dataInicio: '' });
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

	/** Estado do certificado, que da a cor do selo (ver core/formatos). */
	situacao(certificado: Certificado): SituacaoDoCertificado {
		return situacaoDoCertificado(certificado);
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
			return 'Não foi possível falar com o servidor.';
		}
		// O 403 nao ganha mensagem propria: o back-end distingue falta
		// de permissao de falha de token CSRF, e trocar as duas por um
		// texto generico torna o erro indiagnosticavel.
		return falha.error?.mensagem ?? 'Não foi possível completar a operação.';
	}
}
