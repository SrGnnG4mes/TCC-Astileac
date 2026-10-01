import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AutenticacaoService } from '../core/autenticacao.service';
import { ProfissionalService } from '../core/profissional.service';
import { AreaAtuacao, Pagina, ProfissionalFicha, ProfissionalResumo } from '../core/modelos';

/**
 * Consulta do quadro de associados (RF08).
 *
 * E a tela da diretoria e o caso de uso que justifica o sistema: chega
 * uma demanda de interpretacao e e preciso localizar quem esta
 * habilitado para atende-la.
 *
 * A listagem mostra CPF mascarado; o numero completo so aparece ao abrir
 * a ficha. Essa separacao vem do back-end, em dois DTOs distintos -- a
 * tela nao poderia "revelar" o CPF da listagem nem se quisesse, porque
 * ele nao vem na resposta.
 */
@Component({
	selector: 'app-profissionais',
	imports: [ReactiveFormsModule, RouterLink],
	templateUrl: './profissionais.html',
	styleUrl: './profissionais.css'
})
export class Profissionais {

	private readonly formBuilder = inject(FormBuilder);
	private readonly profissionais = inject(ProfissionalService);
	private readonly autenticacao = inject(AutenticacaoService);
	private readonly router = inject(Router);

	readonly usuario = this.autenticacao.usuario;

	readonly filtros = this.formBuilder.nonNullable.group({
		nome: '',
		area: '',
		ativo: ''
	});

	readonly areas = signal<AreaAtuacao[]>([]);
	readonly pagina = signal<Pagina<ProfissionalResumo> | null>(null);
	readonly ficha = signal<ProfissionalFicha | null>(null);
	readonly buscando = signal(false);
	readonly erro = signal<string | null>(null);

	private paginaAtual = 0;

	constructor() {
		this.profissionais.listarAreas().subscribe({
			next: areas => this.areas.set(areas),
			// Sem as areas a busca por nome e situacao continua
			// funcionando; so o seletor fica vazio.
			error: () => this.erro.set('Nao foi possivel carregar as areas de atuacao.')
		});

		this.buscar();
	}

	buscar(): void {
		this.paginaAtual = 0;
		this.consultar();
	}

	irPara(pagina: number): void {
		this.paginaAtual = pagina;
		this.consultar();
	}

	private consultar(): void {
		this.buscando.set(true);
		this.erro.set(null);

		const { nome, area, ativo } = this.filtros.getRawValue();

		this.profissionais.consultar({
			nome,
			area,
			// '' significa "todos": nao vira filtro.
			ativo: ativo === '' ? undefined : ativo === 'true',
			page: this.paginaAtual
		}).subscribe({
			next: pagina => {
				this.pagina.set(pagina);
				this.buscando.set(false);
			},
			error: (falha: HttpErrorResponse) => {
				this.buscando.set(false);
				this.erro.set(this.mensagemDe(falha));
			}
		});
	}

	abrirFicha(id: number): void {
		this.ficha.set(null);
		this.profissionais.buscarPorId(id).subscribe({
			next: ficha => this.ficha.set(ficha),
			error: (falha: HttpErrorResponse) => this.erro.set(this.mensagemDe(falha))
		});
	}

	fecharFicha(): void {
		this.ficha.set(null);
	}

	limpar(): void {
		this.filtros.reset({ nome: '', area: '', ativo: '' });
		this.buscar();
	}

	sair(): void {
		this.autenticacao.sair().subscribe({
			next: () => this.router.navigate(['/login'])
		});
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Nao foi possivel falar com o servidor. Verifique se o back-end esta rodando.';
		}
		if (falha.status === 401) {
			return 'Sua sessao expirou. Entre novamente.';
		}
		return falha.error?.mensagem ?? 'Nao foi possivel completar a consulta.';
	}
}
