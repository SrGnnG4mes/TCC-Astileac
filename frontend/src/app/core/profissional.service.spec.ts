import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProfissionalService } from './profissional.service';

describe('ProfissionalService', () => {
	let servico: ProfissionalService;
	let http: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [provideHttpClient(), provideHttpClientTesting()]
		});
		servico = TestBed.inject(ProfissionalService);
		http = TestBed.inject(HttpTestingController);
	});

	afterEach(() => http.verify());

	describe('consultar (RF08)', () => {
		it('omite da URL os filtros vazios e manda so os preenchidos', () => {
			servico.consultar({ nome: '  maria ', area: '', page: 0 }).subscribe();

			const consulta = http.expectOne(r => r.url === '/api/profissionais');
			const parametros = consulta.request.params;
			expect(parametros.get('nome')).toBe('maria');
			expect(parametros.has('area')).toBe(false);
			expect(parametros.has('ativo')).toBe(false);
			expect(parametros.get('page')).toBe('0');
			expect(parametros.get('size')).toBe('20');
			consulta.flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
		});

		it('manda ativo=false, que e filtro, e nao ausencia de filtro', () => {
			// false e "falsy": um if (filtro.ativo) descartaria justamente
			// a busca pelos inativos.
			servico.consultar({ ativo: false }).subscribe();

			const consulta = http.expectOne(r => r.url === '/api/profissionais');
			expect(consulta.request.params.get('ativo')).toBe('false');
			consulta.flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
		});
	});

	it('desativar e PUT na situacao, nunca DELETE -- a diretoria desativa, nao apaga', () => {
		servico.alterarSituacao(3, false).subscribe();

		const pedido = http.expectOne('/api/profissionais/3/situacao');
		expect(pedido.request.method).toBe('PUT');
		expect(pedido.request.body).toEqual({ ativo: false });
		pedido.flush({});
	});

	it('anexa o documento como multipart, sem Content-Type escrito a mao', () => {
		const arquivo = new File(['%PDF-1.4'], 'certificado.pdf', { type: 'application/pdf' });
		servico.anexarDocumento(3, 9, arquivo).subscribe();

		const envio = http.expectOne({ method: 'POST', url: '/api/profissionais/3/certificados/9/arquivo' });
		expect(envio.request.body).toBeInstanceOf(FormData);
		expect((envio.request.body as FormData).get('arquivo')).toBe(arquivo);
		// O navegador precisa montar o cabecalho com o boundary do
		// multipart; definido aqui, o servidor nao conseguiria ler o corpo.
		expect(envio.request.headers.has('Content-Type')).toBe(false);
		envio.flush({});
	});

	it('o documento e baixado pelo endpoint autorizado, sob /api', () => {
		expect(servico.urlDoDocumento(3, 9)).toBe('/api/profissionais/3/certificados/9/arquivo');
	});
});
