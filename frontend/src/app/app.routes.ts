import { Routes } from '@angular/router';
import { adminGuard, autenticadoGuard } from './core/guardas';

/**
 * Rotas carregadas de forma preguicosa (loadComponent): cada tela vira
 * um pedaco separado do pacote, em vez de tudo no arquivo inicial.
 *
 * As guardas evitam navegacao que vai falhar; a autorizacao de verdade
 * continua no back-end.
 */
export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'login' },
	{
		path: 'login',
		loadComponent: () => import('./login/login').then(m => m.Login)
	},
	{
		path: 'profissionais',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./profissionais/profissionais').then(m => m.Profissionais)
	},
	{
		path: 'painel',
		canActivate: [autenticadoGuard],
		loadComponent: () => import('./painel/painel').then(m => m.Painel)
	},
	{ path: '**', redirectTo: 'login' }
];
