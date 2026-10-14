import { AcaoPipe, DataBrPipe, EntidadePipe, PerfilPipe } from './formatos';

describe('DataBrPipe', () => {
	const formatar = (valor: string | null) => new DataBrPipe().transform(valor);

	it('mostra a data no formato brasileiro', () => {
		expect(formatar('2026-10-20')).toBe('20/10/2026');
	});

	it('primeiro de janeiro continua sendo primeiro de janeiro', () => {
		// Com new Date('2020-01-01'), que e meia-noite em UTC, o fuso do
		// Acre (UTC-5) mostraria 31/12/2019.
		expect(formatar('2020-01-01')).toBe('01/01/2020');
	});

	it('data e hora viram "dd/mm/aaaa às hh:mm", sem segundos nem fracao', () => {
		expect(formatar('2026-10-20T20:00:24.4094037')).toBe('20/10/2026 às 20:00');
	});

	it('valor vazio vira texto vazio', () => {
		expect(formatar(null)).toBe('');
	});
});

describe('rotulos', () => {
	it('a acao do registro de acesso aparece como palavra, com acento', () => {
		expect(new AcaoPipe().transform('EXPORTACAO')).toBe('Exportação');
		expect(new AcaoPipe().transform('CONSULTA')).toBe('Consulta');
	});

	it('o perfil aparece como a associacao fala dele', () => {
		expect(new PerfilPipe().transform('ADMIN')).toBe('diretoria');
		expect(new PerfilPipe().transform('PROFISSIONAL')).toBe('associado');
	});

	it('a entidade do registro de acesso aparece pelo nome', () => {
		expect(new EntidadePipe().transform('PROFISSIONAIS')).toBe('Cadastro do profissional');
	});
});
