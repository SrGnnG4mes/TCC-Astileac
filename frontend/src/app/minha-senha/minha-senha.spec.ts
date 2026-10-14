import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MinhaSenha } from './minha-senha';

/** Troca da propria senha, para qualquer conta. */
describe('MinhaSenha', () => {
	let http: HttpTestingController;
	let tela: MinhaSenha;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [MinhaSenha],
			providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
		});
		http = TestBed.inject(HttpTestingController);
		const fixture = TestBed.createComponent(MinhaSenha);
		tela = fixture.componentInstance;
	});

	afterEach(() => http.verify());

	it('confirmacao diferente da senha nova nao chega ao servidor', () => {
		tela.formulario.setValue({ senhaAtual: 'atual-123', senhaNova: 'nova-senha-1', confirmacao: 'nova-senha-2' });

		tela.trocar();

		expect(tela.formulario.hasError('diferentes')).toBe(true);
		http.expectNone('/api/usuarios/eu/senha');
	});

	it('envia a senha atual e a nova -- a confirmacao fica so na tela', () => {
		tela.formulario.setValue({ senhaAtual: 'atual-123', senhaNova: 'nova-senha-1', confirmacao: 'nova-senha-1' });

		tela.trocar();

		const pedido = http.expectOne({ method: 'POST', url: '/api/usuarios/eu/senha' });
		expect(pedido.request.body).toEqual({ senhaAtual: 'atual-123', senhaNova: 'nova-senha-1' });
		pedido.flush(null, { status: 204, statusText: 'No Content' });

		expect(tela.aviso()).toContain('Senha alterada');
		expect(tela.formulario.getRawValue()).toEqual({ senhaAtual: '', senhaNova: '', confirmacao: '' });
	});

	it('senha atual errada mostra a mensagem do servidor', () => {
		tela.formulario.setValue({ senhaAtual: 'errada', senhaNova: 'nova-senha-1', confirmacao: 'nova-senha-1' });

		tela.trocar();
		http.expectOne('/api/usuarios/eu/senha')
			.flush({ status: 400, mensagem: 'Senha atual incorreta.' }, { status: 400, statusText: 'Bad Request' });

		expect(tela.erro()).toBe('Senha atual incorreta.');
	});
});
