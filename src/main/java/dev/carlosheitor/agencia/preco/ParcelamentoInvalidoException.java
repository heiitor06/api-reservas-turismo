package dev.carlosheitor.agencia.preco;

import dev.carlosheitor.agencia.compartilhado.erro.NegocioException;
import dev.carlosheitor.agencia.compartilhado.erro.TipoProblema;

/** Combinação de forma e parcelas fora da regra. É erro na entrada do cliente, por isso 400. */
public class ParcelamentoInvalidoException extends NegocioException {

	public ParcelamentoInvalidoException(String mensagem) {
		super(TipoProblema.DADOS_INVALIDOS, mensagem);
	}

}
