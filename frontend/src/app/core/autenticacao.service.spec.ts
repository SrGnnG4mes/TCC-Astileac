import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AutenticacaoService } from './autenticacao.service';
import { UsuarioAutenticado } from './modelos';

/**
 * Autenticacao por sessao (RF09).
 *
 * O centro destes testes e a ORDEM das requisicoes do login. Ela ja
 * quebrou duas vezes: sem renovar o CSRF antes, o proprio POST de login
 * voltava 403; sem renovar depois, a primeira escrita apos entrar voltava
 * 403, porque o Spring troca o token ao autenticar.
 */
describe('AutenticacaoService', () => {
	let servico: AutenticacaoService;
	let http: HttpTestingController;

	const diretora: UsuarioAutenticado = { login: 'diretora', perfil: 'ADMIN', profissionalId: null };

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [provideHttpClient(), provideHttpClientTesting()]
		});
		servico = TestBed.inject(AutenticacaoService);
		http = TestBed.inject(HttpTestingController);
	});

	afterEach(() => http.verify());

	it('entrar renova o CSRF antes E depois do login, e so entao pergunta quem e', () => {
		let resultado: UsuarioAutenticado | undefined;
		servico.entrar('diretora', 'senha secreta').subscribe(usuario => (resultado = usuario));

		// 1. Token antes: sem ele o POST de login seria recusado.
		http.expectOne({ method: 'GET', url: '/api/autenticacao/csrf' }).flush({ token: 'antes' });

		// 2. Login como formulario, que e o que o formLogin do Spring le.
		const login = http.expectOne({ method: 'POST', url: '/api/autenticacao/login' });
		expect(login.request.headers.get('Content-Type')).toBe('application/x-www-form-urlencoded');
		expect(new URLSearchParams(login.request.body)).toEqual(
			new URLSearchParams({ login: 'diretora', senha: 'senha secreta' }));
		login.flush('');

		// 3. Token de novo: o Spring trocou o token ao autenticar. Sem esta
		// chamada, a primeira escrita depois de entrar voltava 403.
		http.expectOne({ method: 'GET', url: '/api/autenticacao/csrf' }).flush({ token: 'depois' });

		// 4. So entao pergunta ao servidor quem esta logado.
		http.expectOne({ method: 'GET', url: '/api/autenticacao/eu' }).flush(diretora);

		expect(resultado).toEqual(diretora);
		expect(servico.usuario()).toEqual(diretora);
		expect(servico.eAdministrador()).toBe(true);
	});

	it('login recusado nao renova o token nem marca ninguem como logado', () => {
		let falhou = false;
		servico.entrar('diretora', 'errada').subscribe({ error: () => (falhou = true) });

		http.expectOne('/api/autenticacao/csrf').flush({ token: 'antes' });
		http.expectOne('/api/autenticacao/login').flush(
			{ status: 401, mensagem: 'Credenciais invalidas' },
			{ status: 401, statusText: 'Unauthorized' });

		// http.verify() no afterEach garante que nada mais foi chamado.
		expect(falhou).toBe(true);
		expect(servico.usuario()).toBeNull();
	});

	it('sair esquece quem estava logado', () => {
		servico.carregarUsuario().subscribe();
		http.expectOne('/api/autenticacao/eu').flush(diretora);
		expect(servico.usuario()).toEqual(diretora);

		servico.sair().subscribe();
		http.expectOne({ method: 'POST', url: '/api/autenticacao/logout' }).flush('');

		expect(servico.usuario()).toBeNull();
		expect(servico.eAdministrador()).toBe(false);
	});

	it('associado nao e administrador', () => {
		servico.carregarUsuario().subscribe();
		http.expectOne('/api/autenticacao/eu').flush({ login: 'maria', perfil: 'PROFISSIONAL', profissionalId: 7 });

		expect(servico.eAdministrador()).toBe(false);
	});
});
