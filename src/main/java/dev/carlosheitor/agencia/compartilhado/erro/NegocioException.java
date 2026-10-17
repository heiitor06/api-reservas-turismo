package dev.carlosheitor.agencia.compartilhado.erro;

import java.util.Map;

/**
 * Base das exceções de regra de negócio. O GlobalExceptionHandler trata todas num só lugar:
 * cada subclasse diz qual problema representa e, se quiser, acrescenta propriedades ao ProblemDetail.
 */
public abstract class NegocioException extends RuntimeException {

	private final TipoProblema tipo;

	protected NegocioException(TipoProblema tipo, String mensagem) {
		super(mensagem);
		this.tipo = tipo;
	}

	public TipoProblema tipo() {
		return tipo;
	}

	public Map<String, Object> propriedades() {
		return Map.of();
	}

}
