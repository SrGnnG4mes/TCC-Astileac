import { ChangeDetectorRef, Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, forkJoin, of, throwError } from 'rxjs';
import { ProfissionalService } from '../../core/profissional.service';
import {
	Certificado, Contato, Endereco, Formacao, ProfissionalArea, ProfissionalFicha, Vinculo
} from '../../core/modelos';
import {
	CepPipe, ContatoPipe, DataBrPipe, SeloCertificadoPipe, SituacaoDoCertificado, situacaoDoCertificado
} from '../../core/formatos';

/**
 * Ficha rapida da consulta (RF08): tudo o que esta cadastrado sobre o
 * profissional, so para leitura, numa janela sobre a lista.
 *
 * E o que a diretoria precisa para decidir quem indicar: contatos para
 * falar com a pessoa; areas, formacoes e certificados, que comprovam a
 * habilitacao (Lei 14.704/2023); endereco e vinculos. Sem isso, o caminho
 * era sair da consulta e abrir a ficha completa. Alterar continua sendo na
 * ficha completa ("Editar").
 *
 * Componente proprio, e nao parte da tela de consulta, porque e uma tela
 * dentro da outra: a consulta so lista e chama abrir().
 */
@Component({
	selector: 'app-ficha-rapida',
	imports: [CepPipe, ContatoPipe, DataBrPipe, SeloCertificadoPipe],
	templateUrl: './ficha-rapida.html',
	styleUrl: './ficha-rapida.css'
})
export class FichaRapida {

	private readonly profissionais = inject(ProfissionalService);
	private readonly deteccao = inject(ChangeDetectorRef);
	private readonly janela = viewChild.required<ElementRef<HTMLDialogElement>>('janela');

	/** Mensagem de erro quando a ficha nao carrega; a consulta a mostra. */
	readonly falhou = output<string>();

	readonly ficha = signal<ProfissionalFicha | null>(null);
	readonly contatos = signal<Contato[]>([]);
	readonly endereco = signal<Endereco | null>(null);
	readonly areas = signal<ProfissionalArea[]>([]);
	readonly formacoes = signal<Formacao[]>([]);
	readonly certificados = signal<Certificado[]>([]);
	readonly vinculos = signal<Vinculo[]>([]);

	/**
	 * Le cada parte pelo proprio endpoint, e nao pela exportacao do RF10:
	 * exportar e ato distinto de consultar e fica registrado como tal no
	 * registro de acesso do titular.
	 */
	abrir(id: number): void {
		this.ficha.set(null);
		forkJoin({
			ficha: this.profissionais.buscarPorId(id),
			contatos: this.profissionais.listarContatos(id),
			// 404 e "endereco nao informado", estado normal da relacao 1:1
			// opcional; qualquer outra falha continua sendo erro.
			endereco: this.profissionais.buscarEndereco(id).pipe(
				catchError((falha: HttpErrorResponse) => falha.status === 404 ? of(null) : throwError(() => falha))),
			areas: this.profissionais.listarAreasDo(id),
			formacoes: this.profissionais.listarFormacoes(id),
			certificados: this.profissionais.listarCertificados(id),
			vinculos: this.profissionais.listarVinculos(id)
		}).subscribe({
			next: ({ ficha, contatos, endereco, areas, formacoes, certificados, vinculos }) => {
				// O principal de cada tipo primeiro: e por ele que se tenta antes.
				this.contatos.set([...contatos].sort((a, b) => Number(b.principal) - Number(a.principal)));
				this.endereco.set(endereco);
				this.areas.set(areas);
				this.formacoes.set(formacoes);
				this.certificados.set(certificados);
				this.vinculos.set(vinculos);
				this.ficha.set(ficha);
				// Desenha o conteudo antes de abrir: assim o foco inicial da
				// janela cai no botao "Fechar", e nao numa janela ainda vazia.
				this.deteccao.detectChanges();
				this.janela().nativeElement.showModal();
			},
			error: (falha: HttpErrorResponse) => this.falhou.emit(this.mensagemDe(falha))
		});
	}

	fechar(): void {
		this.janela().nativeElement.close();
	}

	/** Toda forma de fechar passa por aqui: botao, Esc ou clique fora. */
	aoFechar(): void {
		this.ficha.set(null);
	}

	/**
	 * O conteudo ocupa a janela inteira, entao um clique cujo alvo e a
	 * propria janela so pode ter sido no fundo escurecido, fora dela.
	 */
	fecharSeClicouFora(evento: MouseEvent): void {
		if (evento.target === this.janela().nativeElement) {
			this.fechar();
		}
	}

	/** Estado do certificado, que da a cor do selo (ver core/formatos). */
	situacao(certificado: Certificado): SituacaoDoCertificado {
		return situacaoDoCertificado(certificado);
	}

	/** Curso da formacao que o certificado comprova, quando ha uma. */
	nomeDaFormacao(formacaoId: number | null): string | null {
		if (!formacaoId) {
			return null;
		}
		return this.formacoes().find(f => f.id === formacaoId)?.curso ?? null;
	}

	urlDoDocumento(profissionalId: number, certificadoId: number): string {
		return this.profissionais.urlDoDocumento(profissionalId, certificadoId);
	}

	private mensagemDe(falha: HttpErrorResponse): string {
		if (falha.status === 0) {
			return 'Não foi possível falar com o servidor. Verifique se o back-end está rodando.';
		}
		if (falha.status === 401) {
			return 'Sua sessão expirou. Entre novamente.';
		}
		return falha.error?.mensagem ?? 'Não foi possível abrir a ficha.';
	}
}
