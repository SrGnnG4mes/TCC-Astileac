import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Site publico da associacao: a porta de entrada de quem ainda nao e
 * associado e de quem procura um interprete.
 *
 * Fica no mesmo projeto do sistema, e nao num site a parte, porque o
 * "Associe-se" desemboca no fluxo de convite que o sistema ja tem, e a
 * "Area do associado" e o proprio login. Um site separado duplicaria a
 * identidade visual e a hospedagem.
 *
 * NAO ha diretorio publico de interpretes, ao contrario de sites de
 * outras entidades da area. A consulta ao quadro e da diretoria (RF08) e
 * o contato com o profissional acontece fora do sistema -- premissa
 * confirmada com a propria diretoria. Expor nome e area de cada associado
 * a qualquer visitante iria contra o principio da necessidade da LGPD.
 */
@Component({
	selector: 'app-site',
	imports: [RouterOutlet, RouterLink, RouterLinkActive],
	templateUrl: './site.html',
	styleUrl: './site.css'
})
export class Site {

	readonly anoAtual = new Date().getFullYear();

	/**
	 * "Pular para o conteudo" (WCAG 2.4.1): quem usa teclado ou leitor de
	 * tela nao precisa atravessar o menu inteiro em toda pagina. Feito com
	 * foco programatico, e nao com href="#conteudo", porque no Angular um
	 * link de ancora troca a rota para a raiz.
	 */
	irParaConteudo(conteudo: HTMLElement): void {
		conteudo.focus();
	}
}
