package br.edu.ifac.astileac.backend.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base dos testes de integracao: aplicacao inteira contra um PostgreSQL
 * de verdade.
 *
 * Existem por causa de uma classe de defeito que os testes de unidade
 * nao alcancam. O convite de uso unico valia para sempre: os 11 testes
 * do ConviteService passavam, e nao PODIAM falhar, porque com
 * repositorio simulado nao existe contexto de persistencia para ficar
 * obsoleto, nem transacao para confirmar ou desfazer. Defeito de
 * persistencia so aparece com persistencia de verdade.
 *
 * Rodam a parte (./mvnw verify -Pintegracao) e exigem o banco
 * astileac_teste -- ver application-integracao.properties.
 *
 * Os testes NAO sao @Transactional, de proposito. Um teste transacional
 * roda tudo numa transacao so, desfeita no fim, e esconderia justamente
 * o que se quer ver: o que o service de fato grava quando a transacao
 * dele confirma, e o que ele desfaz quando ela falha. Em troca, cada
 * teste comeca limpando o banco.
 */
@SpringBootTest
@ActiveProfiles({ "local", "integracao" })
@Import(Associados.class)
abstract class IntegracaoBase {

	private static final String BANCO_DE_TESTE = "astileac_teste";

	@Autowired
	protected JdbcTemplate jdbc;

	/**
	 * Apaga os dados de todas as tabelas, menos as areas de atuacao (carga
	 * da migration V2) e o historico do Flyway.
	 *
	 * Antes, confere em que banco esta. TRUNCATE nao tem volta: uma
	 * configuracao errada apontando para o banco astileac apagaria o
	 * cadastro da associacao inteiro. Entao o teste se recusa a rodar
	 * fora do banco de teste, em vez de confiar no arquivo de
	 * configuracao.
	 *
	 * As tabelas estao todas listadas, sem CASCADE: se uma tabela nova
	 * apontar para profissional e for esquecida aqui, o TRUNCATE falha em
	 * vez de apagar em cascata algo que ninguem pediu.
	 */
	@BeforeEach
	void limparBanco() {
		String banco = jdbc.queryForObject("SELECT current_database()", String.class);
		if (!BANCO_DE_TESTE.equals(banco)) {
			throw new IllegalStateException(
					"Testes de integracao so rodam no banco " + BANCO_DE_TESTE + ", e nao em " + banco);
		}

		jdbc.execute("""
				TRUNCATE log_acesso, convite, usuario, certificado, formacao,
				         vinculo_empregaticio, profissional_area, contato, endereco,
				         profissional
				RESTART IDENTITY
				""");
	}

	protected long contar(String sql, Object... parametros) {
		Long total = jdbc.queryForObject(sql, Long.class, parametros);
		return total == null ? 0 : total;
	}

}
