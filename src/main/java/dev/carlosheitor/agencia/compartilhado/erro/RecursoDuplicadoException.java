package dev.carlosheitor.agencia.compartilhado.erro;

public class RecursoDuplicadoException extends NegocioException {

	public RecursoDuplicadoException(String mensagem) {
		super(TipoProblema.RECURSO_DUPLICADO, mensagem);
	}

}
