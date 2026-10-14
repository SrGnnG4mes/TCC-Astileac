import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, isObservable } from 'rxjs';
import { adminGuard, autenticadoGuard } from './guardas';
import { AutenticacaoService } from './autenticacao.service';

/**
 * Guardas de rota.
 *
 * Nao sao controle de acesso -- quem autoriza e o back-end. O que se
 * confere aqui e que a interface nao deixa ninguem navegar para uma tela
 * que vai falhar, e que o estado local vazio (pagina recarregada) nao e
 * tratado como "deslogado" sem perguntar ao servidor.
 */
describe('guardas de rota', () => {
	let http: HttpTestingController;
	let router: Router;
	let navegar: ReturnType<typeof vi.spyOn>;

	const rota = {} as ActivatedRouteSnapshot;
	const estado = {} as RouterStateSnapshot;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
		});
		http = TestBed.inject(HttpTestingController);
		router = TestBed.inject(Router);
		navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
	});

	afterEach(() => http.verify());

	/** Executa a guarda e resolve o resultado, seja ele direto ou Observable. */
	async function executar(guarda: typeof adminGuard, resposta?: () => void): Promise<boolean> {
		const resultado = TestBed.runInInjectionContext(() => guarda(rota, estado));
		if (!isObservable(resultado)) {
			return resultado as boolean;
		}
		const promessa = firstValueFrom(resultado as Observable<boolean>);
		resposta?.();
		return promessa;
	}

	function logarComo(perfil: 'ADMIN' | 'PROFISSIONAL'): void {
		TestBed.inject(AutenticacaoService).carregarUsuario().subscribe();
		http.expectOne('/api/autenticacao/eu').flush({ login: 'alguem', perfil, profissionalId: 1 });
	}

	it('recarregar a pagina com a sessao valida nao manda ninguem para o login', async () => {
		// Estado local vazio: a guarda pergunta ao servidor, e o cookie de
		// sessao ainda vale.
		const liberado = await executar(autenticadoGuard, () =>
			http.expectOne('/api/autenticacao/eu').flush({ login: 'maria', perfil: 'PROFISSIONAL', profissionalId: 7 }));

		expect(liberado).toBe(true);
		expect(navegar).not.toHaveBeenCalled();
	});

	it('sem sessao, manda para o login', async () => {
		const liberado = await executar(autenticadoGuard, () =>
			http.expectOne('/api/autenticacao/eu').flush(
				{ status: 401, mensagem: 'Autenticacao necessaria' }, { status: 401, statusText: 'Unauthorized' }));

		expect(liberado).toBe(false);
		expect(navegar).toHaveBeenCalledWith(['/login']);
	});

	it('diretoria entra nas telas administrativas', async () => {
		logarComo('ADMIN');

		expect(await executar(adminGuard)).toBe(true);
		expect(navegar).not.toHaveBeenCalled();
	});

	it('associado e mandado para o painel, e nao para uma tela que daria 403', async () => {
		logarComo('PROFISSIONAL');

		expect(await executar(adminGuard)).toBe(false);
		expect(navegar).toHaveBeenCalledWith(['/painel']);
	});
});
