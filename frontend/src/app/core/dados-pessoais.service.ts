import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from './api';
import { DadosPessoais, ExclusaoResposta, LogAcesso, Pagina } from './modelos';

/**
 * Direitos do titular (RF10) e registro de acesso (RF11).
 *
 * Responde ao achado 13 da revisao de seguranca: o back-end ja atendia
 * ao Art. 18, mas nenhuma tela dava acesso a isso -- e um direito que
 * so o titular pode exercer nao vale nada se ele nao alcanca.
 *
 * O back-end libera estes caminhos ao ADMIN ou ao proprio titular, por
 * @PreAuthorize no controller.
 */
@Injectable({ providedIn: 'root' })
export class DadosPessoaisService {

	private readonly http = inject(HttpClient);

	/** Acesso e portabilidade: tudo que o sistema guarda sobre a pessoa. */
	exportar(profissionalId: number): Observable<DadosPessoais> {
		return this.http.get<DadosPessoais>(`${API}/profissionais/${profissionalId}/dados-pessoais`);
	}

	/**
	 * Pedido de eliminacao.
	 *
	 * Nao apaga na hora: marca o pedido, bloqueia o cadastro e devolve
	 * o prazo de arrependimento. Por isso a resposta tem corpo e vale
	 * mostra-la inteira ao usuario, em vez de um "pronto" generico.
	 */
	solicitarExclusao(profissionalId: number): Observable<ExclusaoResposta> {
		return this.http.delete<ExclusaoResposta>(
			`${API}/profissionais/${profissionalId}/dados-pessoais/exclusao`
		);
	}

	cancelarExclusao(profissionalId: number): Observable<ExclusaoResposta> {
		return this.http.post<ExclusaoResposta>(
			`${API}/profissionais/${profissionalId}/dados-pessoais/exclusao/cancelamento`, null
		);
	}

	/** Quem consultou ou alterou os dados desta pessoa (Art. 46). */
	registroDeAcesso(profissionalId: number, pagina = 0): Observable<Pagina<LogAcesso>> {
		const parametros = new HttpParams().set('page', pagina).set('size', 20);
		return this.http.get<Pagina<LogAcesso>>(
			`${API}/profissionais/${profissionalId}/dados-pessoais/registro-de-acesso`,
			{ params: parametros }
		);
	}
}
