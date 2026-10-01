import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AutenticacaoService } from '../autenticacao.service';

/**
 * Barra de navegacao das telas autenticadas.
 *
 * Existe por uma razao de dominio, nao so para evitar repeticao: um
 * membro da diretoria E TAMBEM um associado. A mesma conta precisa
 * alcancar a administracao do quadro e os proprios direitos do titular,
 * sem trocar de login.
 *
 * Por isso os dois links sao independentes e nao exclusivos:
 *   - "Profissionais" aparece para quem tem perfil ADMIN;
 *   - "Meus dados" aparece para quem tem cadastro de profissional
 *     vinculado (profissionalId preenchido).
 *
 * Quem e diretor e associado ve os dois. A conta de bootstrap, que nao
 * esta vinculada a nenhum profissional, ve so o primeiro -- e correto:
 * ela nao e titular de dado pessoal algum no sistema.
 */
@Component({
	selector: 'app-barra',
	imports: [RouterLink, RouterLinkActive],
	templateUrl: './barra.html',
	styleUrl: './barra.css'
})
export class Barra {

	private readonly autenticacao = inject(AutenticacaoService);
	private readonly router = inject(Router);

	readonly usuario = this.autenticacao.usuario;

	sair(): void {
		this.autenticacao.sair().subscribe({
			next: () => this.router.navigate(['/login']),
			// Se o logout falhar, o cookie pode ter expirado sozinho;
			// mandar para o login e o comportamento util de todo modo.
			error: () => this.router.navigate(['/login'])
		});
	}
}
