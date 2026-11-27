import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AutenticacaoService } from '../core/autenticacao.service';
import { UltimaArea } from '../core/areas';

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

	private readonly autenticacao = inject(AutenticacaoService);
	private readonly ultimaArea = inject(UltimaArea);

	readonly anoAtual = new Date().getFullYear();

	/**
	 * Quem esta logado e veio ao site pela barra do sistema ve "Voltar ao
	 * sistema" no lugar de "Area do associado": mandar para o login quem ja
	 * entrou seria pedir a senha de novo sem motivo.
	 */
	readonly usuario = this.autenticacao.usuario;

	/** Volta para a ultima area visitada; na falta dela, a principal da conta. */
	readonly destinoNoSistema = computed(() => {
		const area = this.ultimaArea.valor()
			?? (this.usuario()?.perfil === 'ADMIN' ? 'diretoria' : 'associado');
		return area === 'diretoria' ? '/profissionais' : '/painel';
	});

	constructor() {
		// Quem abre o site direto (aba nova, recarga) ainda nao tem o estado
		// local: pergunta ao servidor. Para o visitante a resposta e 401, e
		// nada muda -- o site continua igual para quem nao tem conta.
		if (!this.usuario()) {
			this.autenticacao.carregarUsuario().subscribe({ error: () => undefined });
		}
	}

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
