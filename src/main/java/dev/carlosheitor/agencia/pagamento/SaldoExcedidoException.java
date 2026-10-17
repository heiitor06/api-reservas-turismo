package dev.carlosheitor.agencia.pagamento;

import java.math.BigDecimal;
import java.util.Map;

import dev.carlosheitor.agencia.compartilhado.erro.NegocioException;
import dev.carlosheitor.agencia.compartilhado.erro.TipoProblema;

public class SaldoExcedidoException extends NegocioException {

	private final BigDecimal saldo;

	public SaldoExcedidoException(BigDecimal valor, BigDecimal saldo) {
		super(TipoProblema.SALDO_EXCEDIDO, "Pagamento de %s maior que o saldo da reserva (%s)".formatted(valor, saldo));
		this.saldo = saldo;
	}

	@Override
	public Map<String, Object> propriedades() {
		return Map.of("saldo", saldo);
	}

}
