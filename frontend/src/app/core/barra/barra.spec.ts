import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AutenticacaoService } from '../autenticacao.service';
import { Perfil } from '../modelos';
import { Barra } from './barra';

/**
 * Separacao das areas na barra.
 *
 * Antes os menus da diretoria e do associado apareciam juntos numa linha
 * so, e um diretor (que tambem e associado) nao sabia em que papel
 * estava. Agora cada tela mostra so o menu da propria area.
 */
describe('Barra', () => {
	let harness: RouterTestingHarness;

	async function entrar(conta: { perfil: Perfil; profissionalId: number | null }): Promise<void> {
		TestBed.configureTestingModule({
			providers: [
				provideHttpClient(),
				provideHttpClientTesting(),
				// A barra le a area da rota da tela; aqui a propria barra e a tela.
				provideRouter([
					{ path: 'tela-da-diretoria', data: { area: 'diretoria' }, component: Barra },
					{ path: 'tela-do-associado', data: { area: 'associado' }, component: Barra },
					{ path: 'tela-da-conta', component: Barra }
				])
			]
		});
		TestBed.inject(AutenticacaoService).carregarUsuario().subscribe();
		TestBed.inject(HttpTestingController).expectOne('/api/autenticacao/eu')
			.flush({ login: 'diretora', ...conta });
		harness = await RouterTestingHarness.create();
	}

	async function abrir(caminho: string): Promise<HTMLElement> {
		await harness.navigateByUrl(caminho);
		return harness.routeNativeElement as HTMLElement;
	}

	function menu(tela: HTMLElement): string {
		return tela.querySelector('.faixa-area')?.textContent ?? '';
	}

	function areaMarcada(tela: HTMLElement): string | null {
		return tela.querySelector('.areas [aria-current="true"]')?.textContent?.trim() ?? null;
	}

	it('diretor-associado numa tela da diretoria ve so o menu da diretoria', async () => {
		await entrar({ perfil: 'ADMIN', profissionalId: 7 });
		const tela = await abrir('/tela-da-diretoria');

		expect(areaMarcada(tela)).toBe('Diretoria');
		expect(menu(tela)).toContain('Área da diretoria');
		expect(menu(tela)).toContain('Convites');
		expect(menu(tela)).not.toContain('Meu cadastro');
	});

	it('a mesma conta numa tela do associado ve so o menu do associado', async () => {
		await entrar({ perfil: 'ADMIN', profissionalId: 7 });
		const tela = await abrir('/tela-do-associado');

		expect(areaMarcada(tela)).toBe('Minha área');
		expect(menu(tela)).toContain('Minha área de associado');
		expect(menu(tela)).toContain('Meu cadastro');
		expect(menu(tela)).not.toContain('Convites');
	});

	it('na troca de senha, a barra continua na area de onde a pessoa veio', async () => {
		await entrar({ perfil: 'ADMIN', profissionalId: 7 });
		await abrir('/tela-do-associado');
		const tela = await abrir('/tela-da-conta');

		expect(menu(tela)).toContain('Minha área de associado');
	});

	it('quem so tem uma area nao ve o seletor', async () => {
		// Conta de instalacao: diretoria, sem cadastro de associado.
		await entrar({ perfil: 'ADMIN', profissionalId: null });
		const tela = await abrir('/tela-da-diretoria');

		expect(tela.querySelector('.areas')).toBeNull();
		expect(menu(tela)).toContain('Área da diretoria');
	});
});
