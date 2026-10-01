import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AutenticacaoService } from './autenticacao.service';

/**
 * Guardas de rota.
 *
 * Elas NAO sao controle de acesso -- são conveniencia de interface. Quem
 * autoriza de fato e o back-end, pelas regras do SecurityConfig e dos
 * @PreAuthorize; qualquer um pode contornar uma guarda de front-end
 * chamando a API direto. O papel delas e evitar que o usuario navegue
 * para uma tela que vai falhar com 401 ou 403.
 *
 * Confundir as duas coisas e como achar que esconder o CPF na tela
 * protege o dado que continua trafegando na resposta.
 */

/** Exige sessao valida. Pergunta ao servidor; nao confia no estado local. */
export const autenticadoGuard: CanActivateFn = () => {
	const autenticacao = inject(AutenticacaoService);
	const router = inject(Router);

	if (autenticacao.usuario()) {
		return true;
	}

	// Estado local vazio nao significa deslogado: pode ser recarga de
	// pagina com o cookie de sessao ainda valido.
	return autenticacao.carregarUsuario().pipe(
		map(() => true),
		catchError(() => {
			router.navigate(['/login']);
			return of(false);
		})
	);
};

/**
 * Exige perfil ADMIN.
 *
 * Espelha a regra do back-end: GET /profissionais e hasRole('ADMIN').
 * O associado nao navega pelo cadastro dos outros.
 */
export const adminGuard: CanActivateFn = (rota, estado) => {
	const autenticacao = inject(AutenticacaoService);
	const router = inject(Router);

	const decidir = () => {
		if (autenticacao.eAdministrador()) {
			return true;
		}
		router.navigate(['/painel']);
		return false;
	};

	if (autenticacao.usuario()) {
		return decidir();
	}

	return autenticacao.carregarUsuario().pipe(
		map(() => decidir()),
		catchError(() => {
			router.navigate(['/login']);
			return of(false);
		})
	);
};
