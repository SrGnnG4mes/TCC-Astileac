package br.edu.ifac.astileac.backend.controller;

import br.edu.ifac.astileac.backend.dto.CertificadoRequestDTO;
import br.edu.ifac.astileac.backend.dto.CertificadoResponseDTO;
import br.edu.ifac.astileac.backend.security.UsuarioLogado;
import br.edu.ifac.astileac.backend.service.CertificadoService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Certificados do profissional (RF06).
 *
 * Rota aninhada no plural: relacao 1:N, e o registro nao existe fora
 * de um profissional. Listas curtas, entao sem paginacao.
 */
// O associado alcanca so os proprios dados; a diretoria alcanca
// qualquer um. Como todo metodo desta classe recebe profissionalId,
// uma anotacao de classe cobre todos eles (RF09).
@PreAuthorize("hasRole('ADMIN') or @autorizacao.eOProprioProfissional(#profissionalId)")
@RestController
@RequestMapping("/profissionais/{profissionalId}/certificados")
@RequiredArgsConstructor
public class CertificadoController {

	/** Quem pode dar ou retirar o visto: a diretoria, fora do proprio cadastro. */
	private static final String SO_DIRETORIA_E_NAO_O_PROPRIO =
			"hasRole('ADMIN') and !@autorizacao.eOProprioProfissional(#profissionalId)";

	private final CertificadoService certificadoService;
	private final UsuarioLogado usuarioLogado;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CertificadoResponseDTO criar(
			@PathVariable Long profissionalId, @Valid @RequestBody CertificadoRequestDTO dto) {
		return certificadoService.criar(profissionalId, dto);
	}

	@GetMapping
	public List<CertificadoResponseDTO> listar(@PathVariable Long profissionalId) {
		return certificadoService.listar(profissionalId);
	}

	@PutMapping("/{certificadoId}")
	public CertificadoResponseDTO atualizar(
			@PathVariable Long profissionalId,
			@PathVariable Long certificadoId,
			@Valid @RequestBody CertificadoRequestDTO dto) {
		return certificadoService.atualizar(profissionalId, certificadoId, dto);
	}

	@DeleteMapping("/{certificadoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long profissionalId, @PathVariable Long certificadoId) {
		certificadoService.remover(profissionalId, certificadoId);
	}

	// ---------- documento digitalizado (Passo 8) ----------

	/**
	 * Anexa ou substitui o PDF do certificado.
	 *
	 * O associado envia os proprios -- foi o que a diretoria definiu.
	 * A anotacao de classe ja cobre isso: ADMIN ou o proprio titular.
	 *
	 * Substituir o arquivo derruba o reconhecimento da diretoria, se
	 * houver; a regra esta no service.
	 */
	@PostMapping("/{certificadoId}/arquivo")
	public CertificadoResponseDTO anexarDocumento(
			@PathVariable Long profissionalId,
			@PathVariable Long certificadoId,
			@RequestParam("arquivo") MultipartFile arquivo) {
		return certificadoService.anexarDocumento(profissionalId, certificadoId, arquivo);
	}

	/**
	 * Baixa o documento.
	 *
	 * Alcancavel pela diretoria e pelo proprio titular, mais ninguem --
	 * diploma e certificado trazem dado pessoal alem do que o cadastro
	 * ja tem.
	 *
	 * O arquivo e servido por este endpoint, e nao por URL estatica, por
	 * isso mesmo: endereco estatico seria alcancavel por quem adivinhasse
	 * o caminho, sem passar por autorizacao alguma.
	 *
	 * Content-Disposition inline permite abrir no navegador; o nome
	 * oferecido e derivado do titulo, nao do nome interno do arquivo.
	 */
	@GetMapping("/{certificadoId}/arquivo")
	public ResponseEntity<Resource> baixarDocumento(
			@PathVariable Long profissionalId, @PathVariable Long certificadoId) {

		Path caminho = certificadoService.caminhoDoDocumento(profissionalId, certificadoId);

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"inline; filename=\"certificado-" + certificadoId + ".pdf\"")
				.body(new FileSystemResource(caminho));
	}

	// ---------- reconhecimento pela diretoria (RF13) ----------

	/**
	 * Reconhece o documento.
	 *
	 * SOMENTE a diretoria, e nunca sobre o proprio cadastro. A anotacao de
	 * classe permitiria tambem o titular, e deixar alguem validar o proprio
	 * documento esvaziaria o reconhecimento -- o que vale tambem para o
	 * membro da diretoria, que e associado e tem certificados como qualquer
	 * outro: o visto no documento dele precisa vir de outro diretor. O
	 * @PreAuthorize de metodo substitui o da classe.
	 */
	@PreAuthorize(SO_DIRETORIA_E_NAO_O_PROPRIO)
	@PostMapping("/{certificadoId}/validacao")
	public CertificadoResponseDTO validar(
			@PathVariable Long profissionalId, @PathVariable Long certificadoId) {
		return certificadoService.validar(profissionalId, certificadoId, usuarioLogado.loginAtual());
	}

	/**
	 * Desfaz o reconhecimento feito por engano. Mesma regra: so a diretoria,
	 * e nao sobre o proprio documento.
	 */
	@PreAuthorize(SO_DIRETORIA_E_NAO_O_PROPRIO)
	@DeleteMapping("/{certificadoId}/validacao")
	public CertificadoResponseDTO invalidar(
			@PathVariable Long profissionalId, @PathVariable Long certificadoId) {
		return certificadoService.invalidar(profissionalId, certificadoId);
	}

}
