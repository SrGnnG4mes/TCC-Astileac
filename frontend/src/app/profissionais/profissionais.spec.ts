import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Contato, Pagina, ProfissionalFicha, ProfissionalResumo } from '../core/modelos';
import { Profissionais } from './profissionais';

/**
 * Consulta do RF08.
 *
 * A resposta paginada traz os metadados aninhados em "page" (formato
 * VIA_DTO do back-end). Declarados no topo, os campos compilavam e
 * chegavam como undefined: a tela mostrava "NaN". Este teste usa a
 * resposta no formato exato que a API devolve.
 */
describe('Profissionais', () => {
	let http: HttpTestingController;
	let fixture: ComponentFixture<Profissionais>;

	const resposta: Pagina<ProfissionalResumo> = {
		content: [
			{ id: 1, nome: 'Maria Silva', cpfMascarado: '***.***.777-35', ativo: true },
			{ id: 2, nome: 'Joao Pereira', cpfMascarado: '***.***.247-25', ativo: true }
		],
		page: { size: 20, number: 0, totalElements: 2, totalPages: 1 }
	};

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [Profissionais],
			providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
		});
		http = TestBed.inject(HttpTestingController);

		fixture = TestBed.createComponent(Profissionais);
		http.expectOne('/api/areas-atuacao').flush([]);
		http.expectOne(r => r.url === '/api/profissionais').flush(resposta);
		fixture.detectChanges();
	});

	afterEach(() => http.verify());

	it('mostra a contagem a partir dos metadados aninhados em page, sem NaN', () => {
		const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

		expect(texto).toContain('2 profissional(is) encontrado(s)');
		expect(texto).toContain('página 1 de 1');
		expect(texto).not.toContain('NaN');
	});

	it('o filtro de area vai para a consulta', () => {
		fixture.componentInstance.filtros.setValue({ nome: '', area: 'juridica', ativo: '' });
		fixture.componentInstance.buscar();

		const consulta = http.expectOne(r => r.url === '/api/profissionais');
		expect(consulta.request.params.get('area')).toBe('juridica');
		expect(consulta.request.params.has('ativo')).toBe(false);
		consulta.flush(resposta);
	});

	it('ver ficha mostra os contatos, o principal primeiro e o e-mail como link', () => {
		const ficha: ProfissionalFicha = {
			id: 1, nome: 'Maria Silva', cpf: '52998224725', dataNascimento: '1990-05-10',
			dataAssociacao: '2026-10-01', ativo: true, dataExclusaoSolicitada: null, lattesUrl: null
		};
		const contatos: Contato[] = [
			{ id: 7, tipo: 'TELEFONE', valor: '68999998888', principal: false },
			{ id: 8, tipo: 'EMAIL', valor: 'maria@exemplo.org', principal: true }
		];

		fixture.componentInstance.abrirFicha(1);
		http.expectOne('/api/profissionais/1').flush(ficha);
		http.expectOne('/api/profissionais/1/contatos').flush(contatos);
		fixture.detectChanges();

		const tela = fixture.nativeElement as HTMLElement;
		const itens = Array.from(tela.querySelectorAll('.ficha .contatos li')).map(li => li.textContent ?? '');
		expect(itens.length).toBe(2);
		expect(itens[0]).toContain('maria@exemplo.org');
		expect(itens[0]).toContain('principal');
		expect(itens[1]).toContain('(68) 99999-8888');
		expect(tela.querySelector('.ficha a[href="mailto:maria@exemplo.org"]')).not.toBeNull();
	});

	it('ficha sem contato diz que nao ha nenhum registrado', () => {
		fixture.componentInstance.abrirFicha(2);
		http.expectOne('/api/profissionais/2').flush({
			id: 2, nome: 'Joao Pereira', cpf: '11144477735', dataNascimento: '1985-01-20',
			dataAssociacao: '2026-10-01', ativo: true, dataExclusaoSolicitada: null, lattesUrl: null
		});
		http.expectOne('/api/profissionais/2/contatos').flush([]);
		fixture.detectChanges();

		expect((fixture.nativeElement as HTMLElement).querySelector('.ficha')?.textContent).toContain('nenhum registrado');
	});
});
