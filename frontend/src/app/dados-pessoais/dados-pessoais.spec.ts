import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AutenticacaoService } from '../core/autenticacao.service';
import { DadosPessoais } from '../core/modelos';
import { DadosPessoaisTela } from './dados-pessoais';

/**
 * A mesma tela atende o associado ("Meus dados") e a diretoria, que a abre
 * pela consulta para atender ao pedido de um titular. O texto acompanha a
 * area da rota: antes a diretoria lia "Meus dados pessoais" e "Baixar meus
 * dados" sobre outra pessoa, e o diretor nos proprios dados via "Voltar a
 * consulta".
 */
describe('DadosPessoaisTela', () => {
	let http: HttpTestingController;
	let harness: RouterTestingHarness;

	const dados: DadosPessoais = {
		dataDaExportacao: '2026-10-19T10:00:00',
		identificacao: {
			id: 7, nome: 'Maria Silva', cpf: '52998224725', dataNascimento: '1990-05-10',
			dataAssociacao: '2026-10-01', ativo: true
		},
		endereco: null, contatos: [], areasDeAtuacao: [], formacoes: [], certificados: [],
		vinculosEmpregaticios: []
	};

	beforeEach(async () => {
		TestBed.configureTestingModule({
			providers: [
				provideHttpClient(),
				provideHttpClientTesting(),
				provideRouter([
					{ path: 'profissionais/:id/dados-pessoais', data: { area: 'diretoria' }, component: DadosPessoaisTela },
					{ path: 'dados-pessoais', data: { area: 'associado' }, component: DadosPessoaisTela }
				])
			]
		});
		http = TestBed.inject(HttpTestingController);
		// Um diretor que tambem e associado: e nele que as areas se misturavam.
		TestBed.inject(AutenticacaoService).carregarUsuario().subscribe();
		http.expectOne('/api/autenticacao/eu').flush({ login: 'diretora', perfil: 'ADMIN', profissionalId: 7 });
		harness = await RouterTestingHarness.create();
	});

	afterEach(() => http.verify());

	async function abrir(caminho: string): Promise<string> {
		await harness.navigateByUrl(caminho);
		http.expectOne('/api/profissionais/7/dados-pessoais').flush(dados);
		http.expectOne(r => r.url === '/api/profissionais/7/dados-pessoais/registro-de-acesso').flush({
			content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 }
		});
		harness.detectChanges();
		return (harness.routeNativeElement as HTMLElement).querySelector('main')?.textContent ?? '';
	}

	it('pela consulta da diretoria, fala do titular, e nao em primeira pessoa', async () => {
		const texto = await abrir('/profissionais/7/dados-pessoais');

		expect(texto).toContain('Dados pessoais do titular');
		expect(texto).toContain('Baixar os dados do titular');
		expect(texto).toContain('Voltar à consulta');
		expect(texto).not.toContain('Meus dados pessoais');
		expect(texto).not.toContain('Baixar meus dados');
	});

	it('nos proprios dados, o diretor esta na area do associado e nao ve "voltar a consulta"', async () => {
		const texto = await abrir('/dados-pessoais');

		expect(texto).toContain('Meus dados pessoais');
		expect(texto).toContain('Baixar meus dados');
		expect(texto).not.toContain('Voltar à consulta');
		expect(texto).not.toContain('do titular');
	});
});
