package dev.carlosheitor.agencia.preco;

import java.math.BigDecimal;

public record CalculoReserva(
		BigDecimal valorUnitario,
		BigDecimal subtotal,
		BigDecimal percentualDesconto,
		BigDecimal valorTotal) {

	public BigDecimal valorDesconto() {
		return subtotal.subtract(valorTotal);
	}

}
