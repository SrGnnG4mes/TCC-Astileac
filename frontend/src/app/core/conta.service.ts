import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { API } from './api';
import { AutenticacaoService } from './autenticacao.service';
import {
	CadastroPorConvite,
	Conta,
	Convite,
	ConviteEmitido,
	ConviteValidacao,
	Perfil
} from './modelos';

/**
 * Convites e contas de acesso (RF12).
 *
 * Duas chamadas aqui sao PUBLICAS -- validar o convite e cadastrar-se
 * com ele --, porque quem usa o convite ainda nao tem conta. O que faz
 * as vezes de autenticacao nelas e o proprio token, emitido pela
 * diretoria depois da confirmacao do pagamento, de uso unico e com
 * prazo.
 */
@Injectable({ providedIn: 'root' })
export class ContaService {

	private readonly http = inject(HttpClient);
	private readonly autenticacao = inject(AutenticacaoService);

	// ---------- convites (diretoria) ----------

	/**
	 * Emite um convite. O token em texto vem na resposta e so ali: o
	 * banco guarda apenas o hash.
	 */
	emitirConvite(email: string): Observable<ConviteEmitido> {
		return this.http.post<ConviteEmitido>(`${API}/convites`, { email });
	}

	listarConvites(apenasPendentes = false): Observable<Convite[]> {
		const parametros = new HttpParams().set('apenasPendentes', apenasPendentes);
		return this.http.get<Convite[]>(`${API}/convites`, { params: parametros });
	}

	// ---------- cadastro por convite (publico) ----------

	/**
	 * As duas chamadas publicas sao POST, inclusive a validacao, que
	 * logicamente seria GET.
	 *
	 * E para o token do convite NAO ir na URL: caminho e query string
	 * aparecem no log de acesso do servidor, no historico de proxy e no
	 * cabecalho Referer. O token e credencial portadora -- quem o le, usa.
	 *
	 * As duas renovam o CSRF antes de enviar. Quem chega por um link de
	 * e-mail nunca passou pelo login, entao nao tem o cookie XSRF-TOKEN
	 * -- e sem ele o interceptor do Angular nao anexa cabecalho algum e
	 * a chamada volta 403. Descoberto rodando: a primeira visita a tela
	 * de cadastro falhava antes de mostrar qualquer campo.
	 */
	validarConvite(token: string): Observable<ConviteValidacao> {
		return this.autenticacao.renovarCsrf().pipe(
			switchMap(() => this.http.post<ConviteValidacao>(`${API}/cadastro/validacao`, { token }))
		);
	}

	cadastrarPorConvite(dados: CadastroPorConvite): Observable<{ profissionalId: number; mensagem: string }> {
		return this.autenticacao.renovarCsrf().pipe(
			switchMap(() => this.http.post<{ profissionalId: number; mensagem: string }>(
				`${API}/cadastro`, dados))
		);
	}

	// ---------- contas (diretoria) ----------

	listarContas(): Observable<Conta[]> {
		return this.http.get<Conta[]>(`${API}/usuarios`);
	}

	/**
	 * Concede ou retira o perfil administrativo.
	 *
	 * O back-end recusa a operacao que deixaria a associacao sem nenhum
	 * administrador -- so administrador mexe em conta de administrador.
	 */
	alterarPerfil(id: number, perfil: Perfil): Observable<Conta> {
		return this.http.put<Conta>(`${API}/usuarios/${id}/perfil`, { perfil });
	}

	/** Troca a senha de quem esta logado. Exige a senha atual. */
	trocarPropriaSenha(senhaAtual: string, senhaNova: string): Observable<void> {
		return this.http.post<void>(`${API}/usuarios/eu/senha`, { senhaAtual, senhaNova });
	}
}
