import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from './api';
import {
	AreaAtuacao,
	AreaAtuacaoRequest,
	Certificado,
	CertificadoRequest,
	Contato,
	ContatoRequest,
	Endereco,
	EnderecoRequest,
	FiltroProfissional,
	Formacao,
	FormacaoRequest,
	Pagina,
	ProfissionalArea,
	ProfissionalAreaRequest,
	ProfissionalFicha,
	ProfissionalRequest,
	ProfissionalResumo,
	Vinculo,
	VinculoRequest
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

	/** Cadastra um profissional (RF01). Exige perfil ADMIN. */
	criar(dados: ProfissionalRequest): Observable<ProfissionalFicha> {
		return this.http.post<ProfissionalFicha>(`${API}/profissionais`, dados);
	}

	/** Corrige os dados de um profissional -- e o direito de correcao do Art. 18. */
	atualizar(id: number, dados: ProfissionalRequest): Observable<ProfissionalFicha> {
		return this.http.put<ProfissionalFicha>(`${API}/profissionais/${id}`, dados);
	}

	/**
	 * Desativa ou reativa no quadro de associados.
	 *
	 * NAO existe exclusao administrativa: tirar alguem do quadro e
	 * decisao da diretoria sobre a associacao, e apagar destruiria o
	 * registro de que a pessoa FOI associada. Exclusao definitiva so
	 * acontece pelo pedido do titular (RF10).
	 */
	alterarSituacao(id: number, ativo: boolean): Observable<ProfissionalFicha> {
		return this.http.put<ProfissionalFicha>(`${API}/profissionais/${id}/situacao`, { ativo });
	}

	// --- areas de atuacao do profissional (RF04) ---

	listarAreasDo(profissionalId: number): Observable<ProfissionalArea[]> {
		return this.http.get<ProfissionalArea[]>(`${API}/profissionais/${profissionalId}/areas`);
	}

	associarArea(
		profissionalId: number,
		dados: ProfissionalAreaRequest
	): Observable<ProfissionalArea> {
		return this.http.post<ProfissionalArea>(
			`${API}/profissionais/${profissionalId}/areas`, dados
		);
	}

	desassociarArea(profissionalId: number, areaAtuacaoId: number): Observable<void> {
		return this.http.delete<void>(
			`${API}/profissionais/${profissionalId}/areas/${areaAtuacaoId}`
		);
	}

	// --- endereco (RF02) ---

	/**
	 * Busca o endereco. O back-end devolve 404 quando ainda nao ha
	 * endereco -- relacao 1:1 opcional --, e quem chama deve tratar esse
	 * 404 como "nao informado", nao como erro.
	 */
	buscarEndereco(profissionalId: number): Observable<Endereco> {
		return this.http.get<Endereco>(`${API}/profissionais/${profissionalId}/endereco`);
	}

	/**
	 * PUT, e nao POST: cabe um endereco so por profissional, entao a
	 * operacao e "criar ou substituir", idempotente. A rota no singular
	 * diz a mesma coisa.
	 */
	salvarEndereco(profissionalId: number, dados: EnderecoRequest): Observable<Endereco> {
		return this.http.put<Endereco>(`${API}/profissionais/${profissionalId}/endereco`, dados);
	}

	removerEndereco(profissionalId: number): Observable<void> {
		return this.http.delete<void>(`${API}/profissionais/${profissionalId}/endereco`);
	}

	// --- contatos (RF03) ---

	listarContatos(profissionalId: number): Observable<Contato[]> {
		return this.http.get<Contato[]>(`${API}/profissionais/${profissionalId}/contatos`);
	}

	criarContato(profissionalId: number, dados: ContatoRequest): Observable<Contato> {
		return this.http.post<Contato>(`${API}/profissionais/${profissionalId}/contatos`, dados);
	}

	atualizarContato(
		profissionalId: number, contatoId: number, dados: ContatoRequest
	): Observable<Contato> {
		return this.http.put<Contato>(
			`${API}/profissionais/${profissionalId}/contatos/${contatoId}`, dados
		);
	}

	removerContato(profissionalId: number, contatoId: number): Observable<void> {
		return this.http.delete<void>(`${API}/profissionais/${profissionalId}/contatos/${contatoId}`);
	}

	// --- vinculos empregaticios (RF07) ---

	listarVinculos(profissionalId: number): Observable<Vinculo[]> {
		return this.http.get<Vinculo[]>(`${API}/profissionais/${profissionalId}/vinculos`);
	}

	criarVinculo(profissionalId: number, dados: VinculoRequest): Observable<Vinculo> {
		return this.http.post<Vinculo>(`${API}/profissionais/${profissionalId}/vinculos`, dados);
	}

	atualizarVinculo(
		profissionalId: number, vinculoId: number, dados: VinculoRequest
	): Observable<Vinculo> {
		return this.http.put<Vinculo>(
			`${API}/profissionais/${profissionalId}/vinculos/${vinculoId}`, dados
		);
	}

	removerVinculo(profissionalId: number, vinculoId: number): Observable<void> {
		return this.http.delete<void>(`${API}/profissionais/${profissionalId}/vinculos/${vinculoId}`);
	}

	// --- formacoes (RF05) ---

	listarFormacoes(profissionalId: number): Observable<Formacao[]> {
		return this.http.get<Formacao[]>(`${API}/profissionais/${profissionalId}/formacoes`);
	}

	criarFormacao(profissionalId: number, dados: FormacaoRequest): Observable<Formacao> {
		return this.http.post<Formacao>(`${API}/profissionais/${profissionalId}/formacoes`, dados);
	}

	removerFormacao(profissionalId: number, formacaoId: number): Observable<void> {
		return this.http.delete<void>(
			`${API}/profissionais/${profissionalId}/formacoes/${formacaoId}`
		);
	}

	// --- certificados (RF06) ---

	listarCertificados(profissionalId: number): Observable<Certificado[]> {
		return this.http.get<Certificado[]>(`${API}/profissionais/${profissionalId}/certificados`);
	}

	criarCertificado(profissionalId: number, dados: CertificadoRequest): Observable<Certificado> {
		return this.http.post<Certificado>(
			`${API}/profissionais/${profissionalId}/certificados`, dados
		);
	}

	removerCertificado(profissionalId: number, certificadoId: number): Observable<void> {
		return this.http.delete<void>(
			`${API}/profissionais/${profissionalId}/certificados/${certificadoId}`
		);
	}

	// --- documento do certificado (Passo 8) ---

	/**
	 * Anexa ou substitui o PDF.
	 *
	 * Vai como FormData, sem Content-Type definido a mao: o navegador
	 * precisa montar o cabecalho multipart com o boundary, e defini-lo
	 * aqui quebraria o parsing no servidor.
	 *
	 * Substituir o arquivo derruba o reconhecimento da diretoria -- a
	 * regra esta no back-end, e a resposta ja volta com os campos de
	 * validacao limpos.
	 */
	anexarDocumento(
		profissionalId: number, certificadoId: number, arquivo: File
	): Observable<Certificado> {
		const corpo = new FormData();
		corpo.append('arquivo', arquivo);
		return this.http.post<Certificado>(
			`${API}/profissionais/${profissionalId}/certificados/${certificadoId}/arquivo`, corpo
		);
	}

	/**
	 * Endereco do download.
	 *
	 * Serve como href direto: e mesma origem, entao o navegador manda o
	 * cookie de sessao sozinho, e o back-end autoriza. Nao ha URL
	 * estatica para o arquivo justamente por isso -- endereco estatico
	 * seria alcancavel por quem adivinhasse o caminho.
	 */
	urlDoDocumento(profissionalId: number, certificadoId: number): string {
		return `${API}/profissionais/${profissionalId}/certificados/${certificadoId}/arquivo`;
	}

	// --- reconhecimento pela diretoria (RF13) ---

	validarCertificado(profissionalId: number, certificadoId: number): Observable<Certificado> {
		return this.http.post<Certificado>(
			`${API}/profissionais/${profissionalId}/certificados/${certificadoId}/validacao`, null
		);
	}

	invalidarCertificado(profissionalId: number, certificadoId: number): Observable<Certificado> {
		return this.http.delete<Certificado>(
			`${API}/profissionais/${profissionalId}/certificados/${certificadoId}/validacao`
		);
	}

	// --- manutencao da tabela de dominio de areas ---

	criarArea(dados: AreaAtuacaoRequest): Observable<AreaAtuacao> {
		return this.http.post<AreaAtuacao>(`${API}/areas-atuacao`, dados);
	}

	atualizarArea(id: number, dados: AreaAtuacaoRequest): Observable<AreaAtuacao> {
		return this.http.put<AreaAtuacao>(`${API}/areas-atuacao/${id}`, dados);
	}

	/**
	 * Remove uma area do dominio.
	 *
	 * Se houver profissional vinculado, o banco recusa pela chave
	 * estrangeira e o back-end devolve 409 -- comportamento certo: a
	 * area nao some levando vinculos junto.
	 */
	removerArea(id: number): Observable<void> {
		return this.http.delete<void>(`${API}/areas-atuacao/${id}`);
	}
}
