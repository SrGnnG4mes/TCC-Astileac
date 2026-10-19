import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Area } from '../areas';
import { AutenticacaoService } from '../autenticacao.service';
import { Certificado, Endereco, Perfil } from '../modelos';
import { FichaVinculos } from './ficha-vinculos';

/**
 * Ficha do profissional: endereco e certificados.
 *
 * Os casos de endereco reproduzem o defeito achado preenchendo a tela
 * como uma pessoa preencheria: o CEP digitado com hifen nunca salvava,
 * porque o validador exigia o formato do banco antes de a normalizacao
 * rodar. Os de certificado conferem que a tela nao afirma mais do que a
 * diretoria verificou.
 */
describe('FichaVinculos', () => {
	const ID = 3;
	let http: HttpTestingController;
	let fixture: ComponentFixture<FichaVinculos>;
	let ficha: FichaVinculos;

	const semDocumento: Certificado = {
		id: 1, formacaoId: null, titulo: 'Curso A', instituicaoEmissora: 'X', cargaHoraria: 40,
		dataEmissao: '2020-01-01', temDocumento: false, validadoEm: null, validadoPor: null
	};
	const anexado: Certificado = { ...semDocumento, id: 2, titulo: 'Curso B', temDocumento: true };
	const visto: Certificado = {
		...semDocumento, id: 3, titulo: 'Curso C', temDocumento: true,
		validadoEm: '2026-10-01T10:00:00', validadoPor: 'diretora'
	};

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [FichaVinculos],
			providers: [provideHttpClient(), provideHttpClientTesting()]
		});
		http = TestBed.inject(HttpTestingController);
	});

	afterEach(() => http.verify());

	/**
	 * Abre a ficha respondendo as cargas iniciais. `endereco` pode ser o
	 * endereco, ou um status de erro para a busca dele.
	 */
	function abrir(opcoes: {
		endereco?: Endereco | 404 | 500;
		certificados?: Certificado[];
		perfil?: Perfil;
		area?: Area;
	} = {}): void {
		if (opcoes.perfil) {
			TestBed.inject(AutenticacaoService).carregarUsuario().subscribe();
			http.expectOne('/api/autenticacao/eu').flush({ login: 'alguem', perfil: opcoes.perfil, profissionalId: 1 });
		}

		fixture = TestBed.createComponent(FichaVinculos);
		ficha = fixture.componentInstance;
		fixture.componentRef.setInput('profissionalId', ID);
		fixture.componentRef.setInput('area', opcoes.area ?? 'diretoria');
		fixture.detectChanges();

		http.expectOne('/api/areas-atuacao').flush([]);
		const endereco = http.expectOne(`/api/profissionais/${ID}/endereco`);
		const resposta = opcoes.endereco ?? 404;
		if (resposta === 404) {
			endereco.flush(null, { status: 404, statusText: 'Not Found' });
		} else if (resposta === 500) {
			endereco.flush({ status: 500, mensagem: 'Falha ao ler o endereco' }, { status: 500, statusText: 'Error' });
		} else {
			endereco.flush(resposta);
		}
		http.expectOne(`/api/profissionais/${ID}/contatos`).flush([]);
		http.expectOne(`/api/profissionais/${ID}/vinculos`).flush([]);
		http.expectOne(`/api/profissionais/${ID}/areas`).flush([]);
		http.expectOne(`/api/profissionais/${ID}/formacoes`).flush([]);
		http.expectOne(`/api/profissionais/${ID}/certificados`).flush(opcoes.certificados ?? []);
		fixture.detectChanges();
	}

	function texto(): string {
		return (fixture.nativeElement as HTMLElement).textContent ?? '';
	}

	function botoes(rotulo: string): HTMLButtonElement[] {
		return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
			.filter(b => b.textContent?.trim() === rotulo);
	}

	// ---------- endereco (RF02) ----------

	it('endereco ausente (404) e "nao informado", e nao erro', () => {
		abrir({ endereco: 404 });

		expect(ficha.endereco()).toBeNull();
		expect(ficha.erro()).toBeNull();
		expect(texto()).toContain('Nenhum endereço informado');
	});

	it('falha de verdade ao buscar o endereco aparece como erro', () => {
		abrir({ endereco: 500 });

		expect(ficha.erro()).toBe('Falha ao ler o endereco');
	});

	it('CEP com hifen e UF minuscula sao aceitos, e salvos normalizados', () => {
		abrir();
		ficha.abrirEndereco();
		ficha.formEndereco.setValue({
			cep: '69900-000', logradouro: 'Rua Teste', numero: '10', complemento: '',
			bairro: 'Centro', cidade: 'Rio Branco', uf: 'ac'
		});

		// A forma como a pessoa digita tem que passar no formulario...
		expect(ficha.formEndereco.valid).toBe(true);

		ficha.salvarEndereco();

		// ...e chegar ao servidor no formato que ele guarda.
		const envio = http.expectOne({ method: 'PUT', url: `/api/profissionais/${ID}/endereco` });
		expect(envio.request.body.cep).toBe('69900000');
		expect(envio.request.body.uf).toBe('AC');
		envio.flush({ id: 1, ...envio.request.body });
	});

	it('CEP incompleto nao chega ao servidor', () => {
		abrir();
		ficha.abrirEndereco();
		ficha.formEndereco.setValue({
			cep: '69900-00', logradouro: 'Rua Teste', numero: '10', complemento: '',
			bairro: 'Centro', cidade: 'Rio Branco', uf: 'AC'
		});

		ficha.salvarEndereco();

		expect(ficha.formEndereco.valid).toBe(false);
		http.expectNone(`/api/profissionais/${ID}/endereco`);
	});

	// ---------- certificados (RF06, RF13) ----------

	it('cada certificado mostra um dos tres selos, derivados dos dados', () => {
		abrir({ certificados: [semDocumento, anexado, visto] });

		const selos = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.selo'))
			.map(s => s.textContent?.trim());
		expect(selos).toEqual(['sem documento (opcional)', 'documento anexado', 'documento visto']);

		// O selo nao diz "reconhecido" nem "autentico": a diretoria so
		// confere que o documento bate com o declarado.
		expect(texto()).toContain('Não atesta autenticidade');
	});

	it('so a diretoria ve o botao de visto, e so no documento ainda nao visto', () => {
		abrir({ certificados: [semDocumento, anexado, visto], perfil: 'ADMIN' });

		expect(botoes('Dar visto no documento')).toHaveLength(1);
		expect(botoes('Retirar o visto')).toHaveLength(1);
	});

	it('o associado nao ve botao de visto nem no proprio documento', () => {
		abrir({ certificados: [semDocumento, anexado, visto], perfil: 'PROFISSIONAL', area: 'associado' });

		expect(botoes('Dar visto no documento')).toHaveLength(0);
		expect(botoes('Retirar o visto')).toHaveLength(0);
	});

	it('diretor no proprio "Meu cadastro" nao ve o botao de visto', () => {
		// Antes a ficha decidia pelo perfil da conta, e o diretor via o botao
		// de visto nos proprios certificados: as duas areas se misturavam.
		abrir({ certificados: [semDocumento, anexado, visto], perfil: 'ADMIN', area: 'associado' });

		expect(botoes('Dar visto no documento')).toHaveLength(0);
		expect(botoes('Retirar o visto')).toHaveLength(0);
	});

	it('um 403 mostra a mensagem do servidor, e nao um texto generico', () => {
		abrir({ certificados: [anexado], perfil: 'ADMIN' });

		ficha.reconhecer(anexado.id);
		// O back-end distingue falha de CSRF de falta de permissao; trocar
		// as duas por "sem permissao" foi o que escondeu um defeito de CSRF.
		const mensagem = 'Token CSRF ausente ou invalido. Obtenha um novo em GET /api/autenticacao/csrf.';
		http.expectOne(`/api/profissionais/${ID}/certificados/${anexado.id}/validacao`)
			.flush({ status: 403, mensagem }, { status: 403, statusText: 'Forbidden' });

		expect(ficha.erro()).toBe(mensagem);
	});
});
