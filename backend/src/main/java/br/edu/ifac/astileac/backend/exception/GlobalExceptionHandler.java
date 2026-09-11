package br.edu.ifac.astileac.backend.exception;

import br.edu.ifac.astileac.backend.dto.ErroRespostaDTO;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centraliza a traducao de excecoes de negocio/validacao para respostas
 * HTTP consistentes, em vez de cada controller tratar isso na mao.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErroRespostaDTO> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErroRespostaDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
	}

	@ExceptionHandler(CpfJaCadastradoException.class)
	public ResponseEntity<ErroRespostaDTO> tratarCpfDuplicado(CpfJaCadastradoException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErroRespostaDTO(HttpStatus.CONFLICT.value(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroRespostaDTO> tratarValidacao(MethodArgumentNotValidException ex) {
		String mensagem = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErroRespostaDTO(HttpStatus.BAD_REQUEST.value(), mensagem));
	}

}
