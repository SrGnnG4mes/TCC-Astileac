package br.edu.ifac.astileac.backend.exception;

import br.edu.ifac.astileac.backend.dto.ErroRespostaDTO;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centraliza a traducao de excecoes para respostas HTTP consistentes,
 * em vez de cada controller tratar isso na mao.
 *
 * Herda de ResponseEntityExceptionHandler de proposito. Um
 * @RestControllerAdvice solto com um handler generico de Exception
 * captura tambem as excecoes do proprio Spring -- rota inexistente,
 * metodo nao suportado, content-type errado -- e transforma 404, 405 e
 * 415 em 500. A classe pai ja trata cada uma dessas com o status
 * correto; aqui so padronizamos o corpo da resposta e acrescentamos as
 * excecoes de negocio.
 *
 * Regra geral: o cliente recebe o que precisa para corrigir a chamada;
 * o detalhe interno vai para o log do servidor, nunca para a resposta.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	// ---------- excecoes de negocio ----------

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErroRespostaDTO> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
		return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(CpfJaCadastradoException.class)
	public ResponseEntity<ErroRespostaDTO> tratarCpfDuplicado(CpfJaCadastradoException ex) {
		return resposta(HttpStatus.CONFLICT, ex.getMessage());
	}

	/**
	 * Violacao de restricao do banco -- tipicamente tentar remover um
	 * profissional que ainda tem endereco, contato, formacao,
	 * certificado ou vinculo apontando para ele. E conflito de estado
	 * (409), nao erro interno.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroRespostaDTO> tratarViolacaoIntegridade(DataIntegrityViolationException ex) {
		log.warn("Violacao de integridade referencial", ex);
		return resposta(HttpStatus.CONFLICT, "Operacao nao permitida: o registro possui dados vinculados");
	}

	/**
	 * Parametro da URL com tipo errado, ex.: /profissionais/abc.
	 * Mais especifico que o TypeMismatchException tratado pela classe
	 * pai, entao este handler tem precedencia.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErroRespostaDTO> tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Valor invalido para o parametro '" + ex.getName() + "'");
	}

	/**
	 * Rede de seguranca para o que nao foi previsto. As excecoes do
	 * proprio Spring nao caem aqui: a classe pai as trata com handlers
	 * mais especificos, que tem precedencia sobre Exception.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroRespostaDTO> tratarErroInesperado(Exception ex) {
		log.error("Erro nao tratado", ex);
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor");
	}

	// ---------- ajustes no tratamento herdado ----------

	/**
	 * Erros de Bean Validation: lista campo a campo o que falhou, para
	 * o cliente saber o que corrigir.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String mensagem = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.status(status).body(new ErroRespostaDTO(status.value(), mensagem));
	}

	/**
	 * Corpo da requisicao malformado (JSON invalido, data fora do
	 * formato). A mensagem original do Jackson descreve estrutura
	 * interna, entao nao vai para o cliente.
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.status(status)
				.body(new ErroRespostaDTO(status.value(), "Corpo da requisicao invalido ou malformado"));
	}

	/**
	 * Demais excecoes tratadas pela classe pai (404, 405, 415...):
	 * mantem o status que ela definiu e troca o corpo padrao pelo
	 * ErroRespostaDTO, para a API ter um formato de erro so.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.status(status)
				.body(new ErroRespostaDTO(status.value(), textoPadrao(status)));
	}

	private String textoPadrao(HttpStatusCode status) {
		HttpStatus conhecido = HttpStatus.resolve(status.value());
		return conhecido != null ? conhecido.getReasonPhrase() : "Erro na requisicao";
	}

	private ResponseEntity<ErroRespostaDTO> resposta(HttpStatus status, String mensagem) {
		return ResponseEntity.status(status).body(new ErroRespostaDTO(status.value(), mensagem));
	}

}
