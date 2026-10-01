import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, switchMap, tap } from 'rxjs';
import { API } from './api';
import { UsuarioAutenticado } from './modelos';

/**
 * Autenticacao por sessao (RF09).
 *
 * Nao guarda token nenhum. Quem mantem a sessao e o cookie HttpOnly que
 * o servidor escreve no login -- inacessivel a este codigo, o que e
 * justamente a propriedade que se quis ao escolher sessao em vez de
 * token guardado no navegador.
 *
 * O estado aqui e so uma copia local de QUEM esta logado, para a
 * interface decidir o que mostrar. A fonte da verdade continua sendo o
 * servidor: perder este estado (recarregar a pagina) nao desloga
 * ninguem, basta consultar /autenticacao/eu de novo.
 */
@Injectable({ providedIn: 'root' })
export class AutenticacaoService {

	private readonly http = inject(HttpClient);

	private readonly _usuario = signal<UsuarioAutenticado | null>(null);

	/** Somente leitura para fora: quem altera e este servico. */
	readonly usuario = this._usuario.asReadonly();

	/**
	 * Obriga o servidor a escrever o cookie XSRF-TOKEN.
	 *
	 * Desde o Spring Security 6 o token e resolvido de forma preguicosa:
	 * o cookie so e gravado quando alguem LE o token. Sem esta chamada o
	 * cookie nunca existe, o interceptor de CSRF do Angular nao acha
	 * nada para enviar, e o proprio POST de login volta 403.
	 */
	private renovarCsrf(): Observable<void> {
		return this.http.get<{ token: string }>(`${API}/autenticacao/csrf`).pipe(map(() => void 0));
	}

	/**
	 * Entra no sistema.
	 *
	 * Renova o CSRF ANTES do POST, dentro da propria operacao, em vez de
	 * depender de alguem ter chamado isso antes. Essa dependencia de
	 * ordem foi exatamente o que quebrou a colecao do Postman tres
	 * vezes: funcionava na sequencia completa e falhava ao executar uma
	 * requisicao isolada.
	 *
	 * O corpo vai como formulario, nao JSON, porque o formLogin do
	 * Spring le parametros de requisicao -- os nomes "login" e "senha"
	 * estao configurados no SecurityConfig.
	 */
	entrar(login: string, senha: string): Observable<UsuarioAutenticado> {
		const corpo = new URLSearchParams({ login, senha }).toString();
		const cabecalhos = new HttpHeaders({
			'Content-Type': 'application/x-www-form-urlencoded'
		});

		return this.renovarCsrf().pipe(
			switchMap(() => this.http.post(`${API}/autenticacao/login`, corpo, {
				headers: cabecalhos,
				responseType: 'text'
			})),
			// Renova de NOVO, agora depois do login.
			//
			// O Spring troca o token CSRF ao autenticar, como protecao
			// contra fixacao de sessao, e descarta o anterior. Como a
			// resolucao e preguicosa, o cookie so e reescrito quando
			// alguem LE o token -- e nenhuma tela faz isso sozinha.
			//
			// Sem esta segunda chamada, o cookie fica com o valor
			// pre-login e a PRIMEIRA escrita apos entrar no sistema
			// falha com 403; a seguinte funciona, porque a propria
			// falha faz o servidor reescrever o cookie. Confirmado
			// rodando: criar area dava 403 e a mesma chamada repetida
			// dava 201.
			switchMap(() => this.renovarCsrf()),
			switchMap(() => this.carregarUsuario())
		);
	}

	/**
	 * Pergunta ao servidor quem esta logado.
	 *
	 * Usado depois do login e tambem na abertura da aplicacao: se a
	 * sessao anterior continua valida, o usuario nao precisa entrar de
	 * novo.
	 */
	carregarUsuario(): Observable<UsuarioAutenticado> {
		return this.http.get<UsuarioAutenticado>(`${API}/autenticacao/eu`).pipe(
			tap(usuario => this._usuario.set(usuario))
		);
	}

	sair(): Observable<void> {
		return this.http.post(`${API}/autenticacao/logout`, null, { responseType: 'text' }).pipe(
			tap(() => this._usuario.set(null)),
			map(() => void 0)
		);
	}

	eAdministrador(): boolean {
		return this._usuario()?.perfil === 'ADMIN';
	}
}
