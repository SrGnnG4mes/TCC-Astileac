import { Routes } from '@angular/router';
import { adminGuard, autenticadoGuard } from './core/guardas';

/**
 * Rotas carregadas de forma preguicosa (loadComponent): cada tela vira
 * um pedaco separado do pacote, em vez de tudo no arquivo inicial.
 *
 * As guardas evitam navegacao que vai falhar; a autorizacao de verdade
 * continua no back-end.
 *
 * "profissionais/novo" vem antes de "profissionais/:id/..." por
 * clareza; nao ha ambiguidade porque os caminhos tem numero diferente
 * de segmentos.
 */
export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'login' },
	{
		path: 'login',
		loadComponent: () => import('./login/login').then(m => m.Login)
	},
	{
		// UNICA rota sem guarda alem do login: quem usa o convite ainda
		// nao tem conta. O token na query string vem do link de e-mail e
		// e validado contra o servidor antes de a tela mostrar qualquer
		// coisa.
		path: 'cadastro',
		loadComponent: () =>
			import('./cadastro-por-convite/cadastro-por-convite').then(m => m.CadastroPorConvite)
	},
	{
		path: 'profissionais',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./profissionais/profissionais').then(m => m.Profissionais)
	},
	{
		path: 'areas-atuacao',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./areas/areas').then(m => m.Areas)
	},
	{
		path: 'convites',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./convites/convites').then(m => m.Convites)
	},
	{
		path: 'contas',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./contas/contas').then(m => m.Contas)
	},
	{
		path: 'profissionais/novo',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./cadastro/cadastro').then(m => m.Cadastro)
	},
	{
		path: 'profissionais/:id/editar',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./cadastro/cadastro').then(m => m.Cadastro)
	},
	{
		// Autoatendimento. Sem id na rota: o profissional e o que a
		// sessao diz, nunca o que a URL pede.
		path: 'meu-cadastro',
		canActivate: [autenticadoGuard],
		loadComponent: () => import('./meu-cadastro/meu-cadastro').then(m => m.MeuCadastro)
	},
	{
		// Sem id: usa o profissional vinculado a quem esta logado.
		path: 'dados-pessoais',
		canActivate: [autenticadoGuard],
		loadComponent: () =>
			import('./dados-pessoais/dados-pessoais').then(m => m.DadosPessoaisTela)
	},
	{
		// Com id: o back-end permite a ADMIN ou ao proprio titular, e e
		// ele quem decide -- por isso aqui so se exige estar logado.
		path: 'profissionais/:id/dados-pessoais',
		canActivate: [autenticadoGuard],
		loadComponent: () =>
			import('./dados-pessoais/dados-pessoais').then(m => m.DadosPessoaisTela)
	},
	{
		path: 'painel',
		canActivate: [autenticadoGuard],
		loadComponent: () => import('./painel/painel').then(m => m.Painel)
	},
	{ path: '**', redirectTo: 'login' }
];
