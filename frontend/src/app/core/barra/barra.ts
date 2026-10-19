import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AutenticacaoService } from '../autenticacao.service';
import { Area, UltimaArea, areaDaRota } from '../areas';
import { PerfilPipe } from '../formatos';

/**
 * Barra de navegacao das telas autenticadas.
 *
 * Um membro da diretoria E TAMBEM um associado: a mesma conta alcanca a
 * administracao do quadro e o proprio cadastro, sem trocar de login. Antes
 * os dois menus apareciam juntos, numa linha so, e ficava dificil saber
 * em que papel se estava. Agora a barra separa:
 *   - no topo, a conta (login, troca de senha, sair) e, para quem tem as
 *     duas areas, o seletor entre "Diretoria" e "Minha area";
 *   - logo abaixo, uma faixa com o nome da area da tela e SO o menu dela,
 *     com cor propria para cada area.
 *
 * A area vem da rota da tela (core/areas.ts). A conta de instalacao, que
 * nao esta vinculada a nenhum profissional, so tem a area da diretoria; o
 * associado comum, so a dele.
 */
@Component({
	selector: 'app-barra',
	imports: [RouterLink, RouterLinkActive, PerfilPipe],
	templateUrl: './barra.html',
	styleUrl: './barra.css'
})
export class Barra {

	private readonly autenticacao = inject(AutenticacaoService);
	private readonly router = inject(Router);
	private readonly ultimaArea = inject(UltimaArea);

	readonly usuario = this.autenticacao.usuario;

	/** A barra fica dentro de cada tela, entao a rota injetada e a da tela. */
	private readonly areaDaTela = areaDaRota(inject(ActivatedRoute));

	readonly temAsDuasAreas = computed(() => {
		const eu = this.usuario();
		return eu?.perfil === 'ADMIN' && !!eu.profissionalId;
	});

	/**
	 * Area mostrada: a da tela; na troca de senha, a ultima visitada; na
	 * falta das duas, a principal da conta.
	 */
	readonly area = computed<Area>(() =>
		this.areaDaTela
		?? this.ultimaArea.valor()
		?? (this.usuario()?.perfil === 'ADMIN' ? 'diretoria' : 'associado'));

	constructor() {
		if (this.areaDaTela) {
			this.ultimaArea.valor.set(this.areaDaTela);
		}
	}

	sair(): void {
		this.autenticacao.sair().subscribe({
			next: () => this.router.navigate(['/login']),
			// Se o logout falhar, o cookie pode ter expirado sozinho;
			// mandar para o login e o comportamento util de todo modo.
			error: () => this.router.navigate(['/login'])
		});
	}
}
