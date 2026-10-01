import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from './api';
import {
	AreaAtuacao,
	FiltroProfissional,
	Pagina,
	ProfissionalFicha,
	ProfissionalResumo
} from './modelos';

@Injectable({ providedIn: 'root' })
export class ProfissionalService {

	private readonly http = inject(HttpClient);

	/**
	 * Consulta do RF08 -- o caso de uso que justifica o sistema.
	 *
	 * Parametro vazio e OMITIDO da URL em vez de enviado em branco.
	 * O back-end trata nulo e branco igual ("nao filtre por isso"), mas
	 * omitir deixa a URL legivel e torna obvio, ao inspecionar a
	 * requisicao, qual filtro de fato foi aplicado.
	 *
	 * Exige perfil ADMIN: consultar o quadro de associados e atribuicao
	 * da diretoria, nao do associado.
	 */
	consultar(filtro: FiltroProfissional): Observable<Pagina<ProfissionalResumo>> {
		let parametros = new HttpParams();

		if (filtro.nome?.trim()) {
			parametros = parametros.set('nome', filtro.nome.trim());
		}
		if (filtro.area?.trim()) {
			parametros = parametros.set('area', filtro.area.trim());
		}
		if (filtro.ativo !== undefined && filtro.ativo !== null) {
			parametros = parametros.set('ativo', filtro.ativo);
		}
		parametros = parametros.set('page', filtro.page ?? 0);
		parametros = parametros.set('size', filtro.size ?? 20);

		return this.http.get<Pagina<ProfissionalResumo>>(`${API}/profissionais`, { params: parametros });
	}

	/**
	 * Ficha individual, com CPF completo.
	 *
	 * O back-end permite isto ao ADMIN ou ao proprio titular do
	 * registro, decidido por @PreAuthorize no controller.
	 */
	buscarPorId(id: number): Observable<ProfissionalFicha> {
		return this.http.get<ProfissionalFicha>(`${API}/profissionais/${id}`);
	}

	/** Alimenta o seletor de areas da busca. */
	listarAreas(): Observable<AreaAtuacao[]> {
		return this.http.get<AreaAtuacao[]>(`${API}/areas-atuacao`);
	}
}
