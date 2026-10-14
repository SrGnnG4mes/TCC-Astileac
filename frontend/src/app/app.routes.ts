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
/*
 * Cada rota tem titulo proprio (WCAG 2.4.2): e o que aparece na aba e o
 * que o leitor de tela anuncia ao trocar de pagina. Antes, toda tela se
 * chamava "Frontend".
 */
export const routes: Routes = [
	{
		path: 'login',
		title: 'Entrar | ASTILEAC',
		loadComponent: () => import('./login/login').then(m => m.Login)
	},
	{
		// UNICA rota sem guarda alem do login: quem usa o convite ainda
		// nao tem conta. O token na query string vem do link de e-mail e
		// e validado contra o servidor antes de a tela mostrar qualquer
		// coisa.
		path: 'cadastro',
		title: 'Cadastro de associado | ASTILEAC',
		loadComponent: () =>
			import('./cadastro-por-convite/cadastro-por-convite').then(m => m.CadastroPorConvite)
	},
	{
		path: 'profissionais',
		title: 'Profissionais | ASTILEAC',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./profissionais/profissionais').then(m => m.Profissionais)
	},
	{
		path: 'areas-atuacao',
		title: 'Áreas de atuação | ASTILEAC',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./areas/areas').then(m => m.Areas)
	},
	{
		path: 'convites',
		title: 'Convites | ASTILEAC',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./convites/convites').then(m => m.Convites)
	},
	{
		path: 'contas',
		title: 'Contas de acesso | ASTILEAC',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./contas/contas').then(m => m.Contas)
	},
	{
		path: 'profissionais/novo',
		title: 'Novo profissional | ASTILEAC',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./cadastro/cadastro').then(m => m.Cadastro)
	},
	{
		path: 'profissionais/:id/editar',
		title: 'Ficha do profissional | ASTILEAC',
		canActivate: [autenticadoGuard, adminGuard],
		loadComponent: () => import('./cadastro/cadastro').then(m => m.Cadastro)
	},
	{
		// Autoatendimento. Sem id na rota: o profissional e o que a
		// sessao diz, nunca o que a URL pede.
		path: 'meu-cadastro',
		title: 'Meu cadastro | ASTILEAC',
		canActivate: [autenticadoGuard],
		loadComponent: () => import('./meu-cadastro/meu-cadastro').then(m => m.MeuCadastro)
	},
	{
		// Sem id: usa o profissional vinculado a quem esta logado.
		path: 'dados-pessoais',
		title: 'Meus dados | ASTILEAC',
		canActivate: [autenticadoGuard],
		loadComponent: () =>
			import('./dados-pessoais/dados-pessoais').then(m => m.DadosPessoaisTela)
	},
	{
		// Com id: o back-end permite a ADMIN ou ao proprio titular, e e
		// ele quem decide -- por isso aqui so se exige estar logado.
		path: 'profissionais/:id/dados-pessoais',
		title: 'Dados pessoais do titular | ASTILEAC',
		canActivate: [autenticadoGuard],
		loadComponent: () =>
			import('./dados-pessoais/dados-pessoais').then(m => m.DadosPessoaisTela)
	},
	{
		// Qualquer conta troca a propria senha -- inclusive a da diretoria
		// que nao tem cadastro de associado.
		path: 'minha-senha',
		title: 'Alterar senha | ASTILEAC',
		canActivate: [autenticadoGuard],
		loadComponent: () => import('./minha-senha/minha-senha').then(m => m.MinhaSenha)
	},
	{
		path: 'painel',
		title: 'Painel | ASTILEAC',
		canActivate: [autenticadoGuard],
		loadComponent: () => import('./painel/painel').then(m => m.Painel)
	},
	{
		// Site publico: inicio, valores de referencia e associe-se. Sem
		// guarda nenhuma -- e a porta de entrada de quem ainda nao tem
		// conta. Vem depois das rotas do sistema, que tem caminhos proprios.
		path: '',
		loadComponent: () => import('./site/site').then(m => m.Site),
		children: [
			{
				path: '',
				pathMatch: 'full',
				title: 'ASTILEAC | Tradutores, intérpretes e guias-intérpretes de Língua Brasileira de Sinais do Acre',
				loadComponent: () => import('./site/inicio/inicio').then(m => m.Inicio)
			},
			{
				path: 'valores-de-referencia',
				title: 'Valores de referência | ASTILEAC',
				loadComponent: () => import('./site/valores/valores').then(m => m.Valores)
			},
			{
				path: 'associe-se',
				title: 'Associe-se | ASTILEAC',
				loadComponent: () => import('./site/associe-se/associe-se').then(m => m.AssocieSe)
			}
		]
	},
	{ path: '**', redirectTo: '' }
];
