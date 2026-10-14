import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Pagina, ProfissionalResumo } from '../core/modelos';
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
});
