import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
	Certificado, Contato, Endereco, Formacao, ProfissionalArea, ProfissionalFicha, Vinculo
} from '../../core/modelos';
import { FichaRapida } from './ficha-rapida';

/** Ficha rapida da consulta: tudo o que esta cadastrado, numa janela. */
describe('FichaRapida', () => {
	let http: HttpTestingController;
	let fixture: ComponentFixture<FichaRapida>;

	beforeEach(() => {
		// O jsdom nao implementa a janela modal nativa. Para estes testes
		// basta o que a tela usa dela: abrir, fechar e avisar que fechou.
		HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
			this.setAttribute('open', '');
		};
		HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
			this.removeAttribute('open');
			this.dispatchEvent(new Event('close'));
		};

		TestBed.configureTestingModule({
			imports: [FichaRapida],
			providers: [provideHttpClient(), provideHttpClientTesting()]
		});
		http = TestBed.inject(HttpTestingController);
		fixture = TestBed.createComponent(FichaRapida);
		fixture.detectChanges();
	});

	afterEach(() => http.verify());

	const maria: ProfissionalFicha = {
		id: 1, nome: 'Maria Silva', cpf: '52998224725', dataNascimento: '1990-05-10',
		dataAssociacao: '2026-10-01', ativo: true, dataExclusaoSolicitada: null,
		lattesUrl: 'http://lattes.cnpq.br/1234567890123456'
	};

	/**
	 * Abre a ficha rapida e responde as sete leituras que ela faz. O que nao
	 * vier em `partes` responde como cadastro vazio: listas vazias e
	 * endereco nao informado (404).
	 */
	function abrirFicha(ficha: ProfissionalFicha, partes: {
		contatos?: Contato[]; endereco?: Endereco; areas?: ProfissionalArea[];
		formacoes?: Formacao[]; certificados?: Certificado[]; vinculos?: Vinculo[];
	} = {}): HTMLElement {
		const base = `/api/profissionais/${ficha.id}`;
		fixture.componentInstance.abrir(ficha.id);
		http.expectOne(base).flush(ficha);
		http.expectOne(`${base}/contatos`).flush(partes.contatos ?? []);
		const endereco = http.expectOne(`${base}/endereco`);
		if (partes.endereco) {
			endereco.flush(partes.endereco);
		} else {
			endereco.flush(null, { status: 404, statusText: 'Not Found' });
		}
		http.expectOne(`${base}/areas`).flush(partes.areas ?? []);
		http.expectOne(`${base}/formacoes`).flush(partes.formacoes ?? []);
		http.expectOne(`${base}/certificados`).flush(partes.certificados ?? []);
		http.expectOne(`${base}/vinculos`).flush(partes.vinculos ?? []);
		fixture.detectChanges();
		return (fixture.nativeElement as HTMLElement).querySelector('dialog.ficha') as HTMLElement;
	}

	it('mostra os contatos, o principal primeiro e o e-mail como link', () => {
		const janela = abrirFicha(maria, {
			contatos: [
				{ id: 7, tipo: 'TELEFONE', valor: '68999998888', principal: false },
				{ id: 8, tipo: 'EMAIL', valor: 'maria@exemplo.org', principal: true }
			]
		});

		const itens = Array.from(janela.querySelectorAll('.contatos li')).map(li => li.textContent ?? '');
		expect(itens.length).toBe(2);
		expect(itens[0]).toContain('maria@exemplo.org');
		expect(itens[0]).toContain('principal');
		expect(itens[1]).toContain('(68) 99999-8888');
		expect(janela.querySelector('a[href="mailto:maria@exemplo.org"]')).not.toBeNull();
	});

	it('mostra tudo o que esta cadastrado', () => {
		const janela = abrirFicha(maria, {
			endereco: {
				id: 1, cep: '69900000', logradouro: 'Rua das Acacias', numero: '120', complemento: null,
				bairro: 'Centro', cidade: 'Rio Branco', uf: 'AC'
			},
			areas: [{ areaAtuacaoId: 2, nome: 'juridica', dataInicio: '2024-02-01' }],
			formacoes: [{ id: 5, nivel: 'Graduação', curso: 'Letras: Língua Brasileira de Sinais', instituicao: 'UFAC', anoConclusao: 2015 }],
			certificados: [
				{
					id: 9, formacaoId: 5, titulo: 'Proficiência em tradução', instituicaoEmissora: 'MEC',
					cargaHoraria: 120, dataEmissao: '2016-03-10', temDocumento: true,
					validadoEm: '2026-10-01T10:00:00', validadoPor: 'diretora'
				},
				{
					id: 10, formacaoId: null, titulo: 'Interpretação em saúde', instituicaoEmissora: 'IFAC',
					cargaHoraria: 40, dataEmissao: '2020-08-20', temDocumento: false, validadoEm: null, validadoPor: null
				}
			],
			vinculos: [{
				id: 3, instituicao: 'Tribunal de Justiça do Acre', cargo: 'Intérprete', tipoVinculo: 'Efetivo',
				dataInicio: '2018-05-02', dataFim: null, atual: true
			}]
		});
		const texto = janela.textContent ?? '';

		expect(janela.querySelector('a[href="http://lattes.cnpq.br/1234567890123456"]')).not.toBeNull();
		expect(texto).toContain('Rua das Acacias, 120');
		expect(texto).toContain('CEP 69900-000');
		expect(texto).toContain('juridica');
		expect(texto).toContain('Letras: Língua Brasileira de Sinais');
		expect(texto).toContain('comprova Letras: Língua Brasileira de Sinais');
		expect(texto).toContain('visto por diretora');
		expect(texto).toContain('Tribunal de Justiça do Acre');
		expect(texto).toContain('em andamento');

		// Os mesmos selos da ficha completa, e o documento so onde ha documento.
		const selos = Array.from(janela.querySelectorAll('.selo')).map(s => s.textContent?.trim());
		expect(selos).toEqual(['documento visto', 'sem documento (opcional)']);
		expect(janela.querySelectorAll('.certificados a')).toHaveLength(1);
	});

	it('cadastro vazio diz o que falta, em vez de deixar a secao em branco', () => {
		const texto = abrirFicha({ ...maria, lattesUrl: null }).textContent ?? '';

		expect(texto).toContain('Nenhum contato registrado');
		expect(texto).toContain('Nenhum endereço informado');
		expect(texto).toContain('Nenhuma formação registrada');
		expect(texto).toContain('Nenhum certificado registrado');
		expect(texto).toContain('Nenhum vínculo registrado');
	});

	it('abre em janela, e fechar limpa a ficha', () => {
		const janela = (fixture.nativeElement as HTMLElement).querySelector('dialog.ficha') as HTMLDialogElement;
		expect(janela.hasAttribute('open')).toBe(false);

		abrirFicha(maria);

		expect(janela.hasAttribute('open')).toBe(true);
		expect(janela.textContent).toContain('Maria Silva');

		(janela.querySelector('header button') as HTMLButtonElement).click();
		fixture.detectChanges();

		expect(janela.hasAttribute('open')).toBe(false);
		expect(fixture.componentInstance.ficha()).toBeNull();
	});

	it('falha ao carregar avisa a consulta, e a janela nao abre', () => {
		let mensagem: string | null = null;
		fixture.componentInstance.falhou.subscribe(m => mensagem = m);

		fixture.componentInstance.abrir(3);
		http.expectOne('/api/profissionais/3')
			.flush({ status: 404, mensagem: 'Profissional não encontrado: id 3' }, { status: 404, statusText: 'Not Found' });
		http.match(r => r.url.startsWith('/api/profissionais/3/'));

		expect(mensagem).toBe('Profissional não encontrado: id 3');
		expect((fixture.nativeElement as HTMLElement).querySelector('dialog.ficha')?.hasAttribute('open')).toBe(false);
	});
});
