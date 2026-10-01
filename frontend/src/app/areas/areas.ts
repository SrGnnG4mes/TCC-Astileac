import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Barra } from '../core/barra/barra';
import { ProfissionalService } from '../core/profissional.service';
import { AreaAtuacao } from '../core/modelos';

/**
 * Manutencao da tabela de dominio de areas de atuacao.
 *
 * Esta tela e o que torna real a decisao central da modelagem. A relacao
 * profissional-area foi feita com tabela associativa, e nao com colunas
 * booleanas (atua_educacional, atua_juridica...), justamente para que
 * uma area nova nao exija alterar o schema. Sem este formulario, a
 * vantagem existiria no papel e continuaria dependendo de um
 * desenvolvedor rodando SQL.
 *
 * As cinco areas iniciais vieram da migration V2 e da literatura; a
 * diretoria pode precisar de algo mais especifico, e agora consegue.
 */
@Component({
	selector: 'app-areas',
	imports: [ReactiveFormsModule, Barra],
	templateUrl: './areas.html',
	styleUrl: './areas.css'
})
export class Areas {

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);

	readonly areas = signal<AreaAtuacao[]>([]);
	readonly editando = signal<number | null>(null);
	readonly erro = signal<string | null>(null);
	readonly aviso = signal<string | null>(null);

	readonly formulario = this.formBuilder.nonNullable.group({
		nome: ['', [Validators.required, Validators.maxLength(50)]],
		descricao: ['', Validators.maxLength(255)]
	});

	constructor() {
		this.carregar();
	}

	private carregar(): void {
		this.profissionais.listarAreas().subscribe({
			next: lista => this.areas.set(lista),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	editar(area: AreaAtuacao): void {
		this.editando.set(area.id);
		this.formulario.setValue({ nome: area.nome, descricao: area.descricao ?? '' });
		this.erro.set(null);
		this.aviso.set(null);
	}

	cancelarEdicao(): void {
		this.editando.set(null);
		this.formulario.reset({ nome: '', descricao: '' });
	}

	salvar(): void {
		if (this.formulario.invalid) {
			this.formulario.markAllAsTouched();
			return;
		}

		this.erro.set(null);
		this.aviso.set(null);

		const dados = this.formulario.getRawValue();
		const id = this.editando();

		const operacao = id
			? this.profissionais.atualizarArea(id, dados)
			: this.profissionais.criarArea(dados);

		operacao.subscribe({
			next: () => {
				this.cancelarEdicao();
				this.carregar();
				this.aviso.set(id ? 'Área atualizada.' : 'Área criada.');
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	remover(area: AreaAtuacao): void {
		this.erro.set(null);
		this.profissionais.removerArea(area.id).subscribe({
			next: () => {
				this.carregar();
				this.aviso.set('Área removida.');
			},
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	/**
	 * O 409 aqui tem causa conhecida e merece explicacao: a area esta
	 * vinculada a algum profissional e a chave estrangeira recusa a
	 * remocao. E o comportamento certo -- a area nao some levando os
	 * vinculos junto --, mas a mensagem generica do back-end nao diz o
	 * que fazer.
	 */
	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor.';
		}
		if (falha.status === 409) {
			return 'Esta área está vinculada a profissionais. '
				+ 'Desvincule-a deles antes de remover, ou mantenha-a e apenas corrija o nome.';
		}
		// O 403 NAO ganha mensagem propria aqui. O back-end distingue
		// falta de permissao de falha de token CSRF justamente para o
		// erro ser diagnosticavel; trocar isso por um texto generico
		// joga fora a distincao -- foi o que me fez ler uma falha de
		// CSRF como problema de perfil.
		return falha.error?.mensagem ?? 'Nao foi possivel completar a operacao.';
	}
}
