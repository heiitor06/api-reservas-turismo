package dev.carlosheitor.agencia.compartilhado.erro;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
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

	private static final String VIOLACAO_UNIQUE = "23505";

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

	/**
	 * Violação de UNIQUE (SQLState 23505) acontece quando duas requisições passam juntas pela checagem
	 * "já existe?" do service: é conflito do cliente, não erro do servidor. Outras violações (FK, CHECK)
	 * indicam bug no código e continuam como 500.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> tratarIntegridade(DataIntegrityViolationException ex) {
		if (ex.getMostSpecificCause() instanceof SQLException sql && VIOLACAO_UNIQUE.equals(sql.getSQLState())) {
			ProblemDetail problema = TipoProblema.RECURSO_DUPLICADO.problema("Já existe um registro com esses dados.");
			return ResponseEntity.status(TipoProblema.RECURSO_DUPLICADO.status()).body(problema);
		}
		return tratarInesperado(ex);
	}

	/**
	 * Não conseguiu o lock a tempo (fila longa na mesma saída) ou o banco escolheu esta transação
	 * para desfazer um deadlock. Nada foi gravado, então repetir a requisição é seguro.
	 */
	@ExceptionHandler(PessimisticLockingFailureException.class)
	ResponseEntity<ProblemDetail> tratarLockIndisponivel(PessimisticLockingFailureException ex) {
		log.warn("Lock indisponível: {}", ex.getMostSpecificCause().getMessage());
		ProblemDetail problema = TipoProblema.TENTE_NOVAMENTE.problema(
				"Muitas operações simultâneas neste recurso. Tente novamente em instantes.");
		return ResponseEntity.status(TipoProblema.TENTE_NOVAMENTE.status())
				.header(HttpHeaders.RETRY_AFTER, "1")
				.body(problema);
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
