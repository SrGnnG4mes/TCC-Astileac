package br.edu.ifac.astileac.backend.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifac.astileac.backend.service.CertificadoService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * O PDF do certificado acompanha o registro (Passo 8 / RF13).
 *
 * O arquivo fica em disco, fora da transacao do banco. O que se confere
 * aqui e que os dois nao se desencontram: registro apagado leva o
 * documento junto, e documento substituido nao deixa o anterior para
 * tras.
 */
class DocumentoDoCertificadoIT extends IntegracaoBase {

	@Autowired
	private CertificadoService certificadoService;

	@Autowired
	private Associados associados;

	@Value("${app.uploads.dir}")
	private String pastaDeUploads;

	@Test
	void removerOCertificadoApagaODocumento() {
		Long id = associados.completo("Maria Silva", "11144477735", "maria.silva");
		Long certificadoId = certificadoDe(id);
		Path documento = documentoDe(id);
		assertTrue(Files.exists(documento));

		certificadoService.remover(id, certificadoId);

		assertEquals(0, contar("SELECT count(*) FROM certificado WHERE id = ?", certificadoId));
		assertFalse(Files.exists(documento), "o documento ficou orfao no disco depois de o certificado ser removido");
	}

	@Test
	void substituirODocumentoApagaOAnterior() {
		Long id = associados.completo("Maria Silva", "11144477735", "maria.silva");
		Path anterior = documentoDe(id);

		certificadoService.anexarDocumento(id, certificadoDe(id), Associados.pdf());

		Path atual = documentoDe(id);
		assertNotEquals(anterior, atual);
		assertTrue(Files.exists(atual));
		assertFalse(Files.exists(anterior));
	}

	private Long certificadoDe(Long id) {
		return jdbc.queryForObject("SELECT id FROM certificado WHERE profissional_id = ?", Long.class, id);
	}

	private Path documentoDe(Long id) {
		String caminho = jdbc.queryForObject(
				"SELECT arquivo_path FROM certificado WHERE profissional_id = ?", String.class, id);
		return Path.of(pastaDeUploads).toAbsolutePath().resolve(caminho).normalize();
	}

}
