package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda e le os documentos digitalizados dos certificados (Passo 8).
 *
 * Fica separado do CertificadoService de proposito: um cuida de regra de
 * negocio, o outro de sistema de arquivos. Sao preocupacoes com modos de
 * falha diferentes.
 *
 * O banco guarda o CAMINHO, nunca o binario -- decisao do modelo desde o
 * inicio.
 */
@Service
@Slf4j
public class ArquivoCertificadoService {

	/** Assinatura de um PDF: os cinco primeiros bytes sao "%PDF-". */
	private static final byte[] ASSINATURA_PDF = { 0x25, 0x50, 0x44, 0x46, 0x2D };

	private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;

	private final Path raiz;

	public ArquivoCertificadoService(@Value("${app.uploads.dir}") String diretorio) {
		// normalize() resolve ".." que por acaso esteja na configuracao;
		// toAbsolutePath() torna a comparacao de contencao confiavel.
		this.raiz = Path.of(diretorio, "certificados").toAbsolutePath().normalize();
	}

	/**
	 * Grava o arquivo e devolve o caminho relativo a ser guardado no
	 * banco.
	 *
	 * O NOME DO ARQUIVO E GERADO AQUI, nunca aproveitado do que o cliente
	 * enviou. Nome vindo do cliente e o vetor classico de path traversal:
	 * um "../../application-local.properties" escreveria fora da pasta de
	 * uploads. Gerando um UUID, nao existe nome do cliente a sanear.
	 */
	public String guardar(MultipartFile arquivo) {
		validar(arquivo);

		String nome = UUID.randomUUID() + ".pdf";
		Path destino = raiz.resolve(nome).normalize();

		// Defesa em profundidade: mesmo com nome gerado, confirma que o
		// destino caiu dentro da raiz antes de escrever.
		if (!destino.startsWith(raiz)) {
			throw new IllegalStateException("Caminho de destino fora da pasta de uploads");
		}

		try {
			Files.createDirectories(raiz);
			try (InputStream entrada = arquivo.getInputStream()) {
				Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			log.error("Falha ao gravar documento de certificado", e);
			throw new RegraDeNegocioException("Nao foi possivel guardar o arquivo enviado");
		}

		return "certificados/" + nome;
	}

	/**
	 * Resolve um caminho guardado no banco para leitura.
	 *
	 * Revalida a contencao na raiz. Parece redundante, ja que o caminho
	 * foi gerado por esta classe -- mas o valor passou pelo banco, e
	 * tratar o que volta do banco como confiavel e a suposicao que
	 * costuma envelhecer mal.
	 */
	public Path resolver(String caminhoRelativo) {
		Path arquivo = raiz.getParent().resolve(caminhoRelativo).toAbsolutePath().normalize();

		if (!arquivo.startsWith(raiz)) {
			log.error("Caminho de certificado fora da pasta de uploads: {}", caminhoRelativo);
			throw new RegraDeNegocioException("Arquivo indisponivel");
		}

		if (!Files.isReadable(arquivo)) {
			throw new RegraDeNegocioException("O arquivo deste certificado nao esta disponivel");
		}

		return arquivo;
	}

	/**
	 * Apaga o arquivo so depois que a transacao em curso confirmar.
	 *
	 * Arquivo em disco nao participa da transacao do banco. Apagando na
	 * hora, uma falha posterior desfaria o banco mas nao o disco: o
	 * registro voltaria apontando para um documento que nao existe mais.
	 * Esperando a confirmacao, o pior caso se inverte -- um arquivo
	 * orfao, que e desperdicio de disco, e nao documento perdido.
	 */
	public void apagarAoConfirmar(String caminhoRelativo) {
		if (caminhoRelativo == null) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			apagar(caminhoRelativo);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				apagar(caminhoRelativo);
			}
		});
	}

	private void apagar(String caminhoRelativo) {
		if (caminhoRelativo == null) {
			return;
		}
		try {
			Files.deleteIfExists(resolver(caminhoRelativo));
		} catch (IOException | RegraDeNegocioException e) {
			// Arquivo orfao e desperdicio de disco, nao falha de
			// operacao: a alteracao ja foi confirmada no banco.
			log.warn("Nao foi possivel apagar o documento: {}", caminhoRelativo);
		}
	}

	/**
	 * Aceita somente PDF, e confere a ASSINATURA do conteudo, nao so o
	 * content-type e a extensao -- os dois vem do cliente e sao
	 * declaracao, nao prova.
	 */
	private void validar(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new RegraDeNegocioException("Envie um arquivo");
		}

		if (arquivo.getSize() > TAMANHO_MAXIMO) {
			throw new RegraDeNegocioException("O arquivo deve ter no maximo 5 MB");
		}

		byte[] inicio = new byte[ASSINATURA_PDF.length];
		try (InputStream entrada = arquivo.getInputStream()) {
			if (entrada.read(inicio) != ASSINATURA_PDF.length) {
				throw new RegraDeNegocioException("Envie o certificado em PDF");
			}
		} catch (IOException e) {
			throw new RegraDeNegocioException("Nao foi possivel ler o arquivo enviado");
		}

		for (int i = 0; i < ASSINATURA_PDF.length; i++) {
			if (inicio[i] != ASSINATURA_PDF[i]) {
				throw new RegraDeNegocioException("Envie o certificado em PDF");
			}
		}
	}
}
