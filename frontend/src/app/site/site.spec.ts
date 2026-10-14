import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';

/**
 * Site publico.
 *
 * O site ocupa a rota raiz, com rotas filhas. O risco e ele capturar
 * caminhos do sistema -- o login, por exemplo -- e o visitante cair no
 * site em vez da tela que pediu. Por isso os testes navegam pelas rotas
 * de verdade da aplicacao.
 */
describe('site publico', () => {
	let harness: RouterTestingHarness;

	beforeEach(async () => {
		TestBed.configureTestingModule({
			providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()]
		});
		harness = await RouterTestingHarness.create();
	});

	function texto(): string {
		return (harness.routeNativeElement as HTMLElement | null)?.textContent ?? '';
	}

	it('a raiz abre o inicio do site, e nao o login', async () => {
		await harness.navigateByUrl('/');

		expect(texto()).toContain('Tradutores e intérpretes de Libras do Acre');
		expect(texto()).toContain('Área do associado');
	});

	it('o login continua sendo o login, e nao uma pagina do site', async () => {
		await harness.navigateByUrl('/login');

		expect((harness.routeNativeElement as HTMLElement).querySelector('input#senha')).not.toBeNull();
		expect(texto()).not.toContain('Valores de referência');
	});

	it('enquanto nao houver valores, a pagina diz que serao publicados, sem numero inventado', async () => {
		await harness.navigateByUrl('/valores-de-referencia');

		expect(texto()).toContain('está sendo definida pela diretoria');
		expect(texto()).not.toContain('R$');
	});

	it('sem canal de contato preenchido, nao aparece contato inventado', async () => {
		await harness.navigateByUrl('/associe-se');

		expect(texto()).toContain('Os canais de contato da diretoria serão publicados em breve');
		expect((harness.routeNativeElement as HTMLElement).querySelector('a[href^="mailto:"]')).toBeNull();
	});

	it('o associe-se descreve o convite de uso unico com o prazo que o sistema aplica', async () => {
		await harness.navigateByUrl('/associe-se');

		// O prazo vem do ConviteService.DIAS_DE_VALIDADE do back-end. Se
		// um dos dois mudar, este teste lembra de mudar o outro.
		expect(texto()).toContain('vale por 7 dias e serve uma única vez');
	});

	it('caminho inexistente volta para o inicio do site', async () => {
		await harness.navigateByUrl('/pagina-que-nao-existe');

		expect(texto()).toContain('Tradutores e intérpretes de Libras do Acre');
	});
});
