package dev.carlosheitor.agencia.compartilhado.erro;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte toda exceção em ProblemDetail. Estende ResponseEntityExceptionHandler para herdar
 * o tratamento padrão do Spring MVC (405, 415 etc.) e sobrescreve só os 400 que a API padroniza.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NegocioException.class)
	ResponseEntity<ProblemDetail> tratarNegocio(NegocioException ex) {
		ProblemDetail problema = ex.tipo().problema(ex.getMessage());
		ex.propriedades().forEach(problema::setProperty);
		return ResponseEntity.status(ex.tipo().status()).body(problema);
	}

	@ExceptionHandler(PropertyReferenceException.class)
	ResponseEntity<ProblemDetail> tratarOrdenacaoInvalida(PropertyReferenceException ex) {
		ProblemDetail problema = TipoProblema.DADOS_INVALIDOS.problema("Campo de ordenação inválido: " + ex.getPropertyName());
		return ResponseEntity.badRequest().body(problema);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> tratarInesperado(Exception ex) {
		log.error("Erro inesperado", ex);
		ProblemDetail problema = TipoProblema.ERRO_INTERNO.problema("Ocorreu um erro inesperado. Tente novamente mais tarde.");
		return ResponseEntity.internalServerError().body(problema);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErroCampo> erros = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
				.sorted(Comparator.comparing(ErroCampo::campo))
				.toList();
		ProblemDetail problema = TipoProblema.DADOS_INVALIDOS.problema("Um ou mais campos são inválidos.");
		problema.setProperty("erros", erros);
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = TipoProblema.DADOS_INVALIDOS.problema("Corpo da requisição ausente ou malformado.");
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = TipoProblema.DADOS_INVALIDOS.problema("Valor inválido: " + ex.getValue());
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

}
