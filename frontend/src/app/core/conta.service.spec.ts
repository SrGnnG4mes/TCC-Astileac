import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ContaService } from './conta.service';

/**
 * Cadastro por convite (RF12), do lado do visitante.
 *
 * Quem chega pelo link do e-mail nunca leu o token CSRF, entao nao tem o
 * cookie -- e as duas chamadas publicas sao POST. Foi o terceiro episodio
 * do mesmo defeito: a tela de cadastro falhava com 403 antes de mostrar
 * qualquer campo. Cada operacao renova o token dentro de si mesma.
 */
describe('ContaService', () => {
	let servico: ContaService;
	let http: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [provideHttpClient(), provideHttpClientTesting()]
		});
		servico = TestBed.inject(ContaService);
		http = TestBed.inject(HttpTestingController);
	});

	afterEach(() => http.verify());

	it('validar o convite renova o CSRF antes, e o token vai no corpo, nao na URL', () => {
		servico.validarConvite('codigo-do-convite').subscribe();

		http.expectOne({ method: 'GET', url: '/api/autenticacao/csrf' }).flush({ token: 'x' });

		const validacao = http.expectOne({ method: 'POST', url: '/api/cadastro/validacao' });
		// URL aparece em log de servidor, proxy e cabecalho Referer.
		expect(validacao.request.urlWithParams).not.toContain('codigo-do-convite');
		expect(validacao.request.body).toEqual({ token: 'codigo-do-convite' });
		validacao.flush({ valido: true, email: 'maria@exemplo.com', motivo: null });
	});

	it('cadastrar pelo convite tambem renova o CSRF antes', () => {
		const dados = {
			token: 'codigo-do-convite',
			nome: 'Maria Silva',
			cpf: '11144477735',
			dataNascimento: '1990-05-20',
			login: 'maria.silva',
			senha: 'senha-de-teste-123'
		};
		servico.cadastrarPorConvite(dados).subscribe();

		http.expectOne({ method: 'GET', url: '/api/autenticacao/csrf' }).flush({ token: 'x' });

		const cadastro = http.expectOne({ method: 'POST', url: '/api/cadastro' });
		expect(cadastro.request.body).toEqual(dados);
		cadastro.flush({ profissionalId: 1, mensagem: 'ok' });
	});
});
