package dev.carlosheitor.agencia.compartilhado.erro;

public class OperacaoInvalidaException extends NegocioException {

	public OperacaoInvalidaException(String mensagem) {
		super(TipoProblema.OPERACAO_INVALIDA, mensagem);
	}

}
