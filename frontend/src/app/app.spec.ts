import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [App],
			// O App e so o router-outlet; sem rotas o componente nao
			// consegue nem ser criado.
			providers: [provideRouter(routes)]
		}).compileComponents();
	});

	it('deve ser criado', () => {
		const fixture = TestBed.createComponent(App);
		expect(fixture.componentInstance).toBeTruthy();
	});
});
