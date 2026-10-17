package dev.carlosheitor.agencia.compartilhado.erro;

public class RecursoNaoEncontradoException extends NegocioException {

	public RecursoNaoEncontradoException(String recurso, Object id) {
		super(TipoProblema.RECURSO_NAO_ENCONTRADO, recurso + " " + id + " não existe");
	}

}
