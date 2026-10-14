import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../core/autenticacao.service';
import { Barra } from '../core/barra/barra';
import { FichaVinculos } from '../core/ficha-vinculos/ficha-vinculos';
import { ProfissionalService } from '../core/profissional.service';
import { naoFutura, DATA_MINIMA, hoje } from '../core/validadores';

/**
 * Autoatendimento: o associado mantem o proprio cadastro.
 *
 * Atende ao que a diretoria pediu -- "o maior trabalho do lado de quem
 * esta se associando" -- e ao direito de correcao do Art. 18.
 *
 * O back-end ja autorizava isto desde o RF09: os controllers de
 * profissional, areas, formacoes e certificados sao
 * `hasRole('ADMIN') or @autorizacao.eOProprioProfissional(...)`. O que
 * faltava era a tela: a rota de edicao existente esta atras do
 * adminGuard, porque e a da diretoria administrando qualquer cadastro.
 * Mesma forma do achado 13 -- a permissao existia, o caminho nao.
 *
 * Nao recebe id na rota: o profissional e o que a sessao diz, nunca o
 * que a URL pede. Assim nao existe a forma de digitar o id de outra
 * pessoa nesta tela.
 */
@Component({
	selector: 'app-meu-cadastro',
	imports: [ReactiveFormsModule, RouterLink, Barra, FichaVinculos],
	templateUrl: './meu-cadastro.html',
	styleUrl: './meu-cadastro.css'
})
export class MeuCadastro {

	/**
	 * Limites dos campos de data. Com o max, o navegador deixa o ano com 4
	 * digitos e nao oferece data futura no calendario.
	 */
	readonly dataMinima = DATA_MINIMA;
	readonly hoje = hoje();

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);
	private readonly autenticacao = inject(AutenticacaoService);

	readonly profissionalId = signal<number | null>(null);
	/** Guardados do carregamento para reenviar inalterados -- ver salvar(). */
	readonly cpf = signal<string | null>(null);
	private readonly dataAssociacao = signal<string | null>(null);
	readonly salvando = signal(false);
	readonly erro = signal<string | null>(null);
	readonly aviso = signal<string | null>(null);

	readonly formulario = this.formBuilder.nonNullable.group({
		nome: ['', [Validators.required, Validators.maxLength(150)]],
		dataNascimento: ['', [Validators.required, naoFutura]],
		lattesUrl: ['', Validators.maxLength(255)]
	});

	constructor() {
		const id = this.autenticacao.usuario()?.profissionalId ?? null;
		this.profissionalId.set(id);

		if (id === null) {
			// Conta administrativa sem cadastro de associado vinculado,
			// como a de bootstrap. Nao e erro: ela simplesmente nao e
			// titular de dado pessoal algum no sistema.
			this.erro.set('Esta conta não está vinculada a um cadastro de profissional.');
			return;
		}

		this.carregar(id);
	}

	private carregar(id: number): void {
		this.profissionais.buscarPorId(id).subscribe({
			next: ficha => {
				this.cpf.set(ficha.cpf);
				this.dataAssociacao.set(ficha.dataAssociacao);
				this.formulario.patchValue({
					nome: ficha.nome,
					lattesUrl: ficha.lattesUrl ?? '',
					dataNascimento: ficha.dataNascimento
				});
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	/**
	 * Salva nome e data de nascimento.
	 *
	 * O CPF e a data de associacao vao inalterados, porque o DTO de
	 * entrada exige os quatro campos. Nao sao editaveis aqui de
	 * proposito:
	 *
	 *  - o CPF e a ancora de identidade, conferida pela associacao no
	 *    momento do pagamento e usada para impedir cadastro duplicado;
	 *  - a data de associacao e definida pela entidade ao reconhecer o
	 *    pagamento, nao pelo associado.
	 *
	 * Correcao de CPF errado continua sendo direito do titular (Art. 18,
	 * III) e e feita pela diretoria, que alcanca os dois campos na tela
	 * dela. Esta restricao e de interface: a API permite ao titular, e
	 * quem autoriza continua sendo o servidor.
	 */
	salvar(): void {
		const id = this.profissionalId();
		const cpf = this.cpf();
		const associacao = this.dataAssociacao();

		if (id === null || cpf === null || associacao === null || this.formulario.invalid) {
			this.formulario.markAllAsTouched();
			return;
		}

		this.salvando.set(true);
		this.erro.set(null);
		this.aviso.set(null);

		const dados = this.formulario.getRawValue();

		this.profissionais.atualizar(id, {
			nome: dados.nome,
			cpf: cpf,
			lattesUrl: dados.lattesUrl,
			dataNascimento: dados.dataNascimento,
			dataAssociacao: associacao
		}).subscribe({
			next: () => {
				this.salvando.set(false);
				this.aviso.set('Dados atualizados.');
			},
			error: (falha: HttpErrorResponse) => {
				this.salvando.set(false);
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Não foi possível falar com o servidor.';
		}
		// O 403 nao ganha mensagem propria: o back-end distingue falta
		// de permissao de falha de token CSRF, e trocar as duas por um
		// texto generico torna o erro indiagnosticavel.
		return falha.error?.mensagem ?? 'Não foi possível salvar.';
	}
}
