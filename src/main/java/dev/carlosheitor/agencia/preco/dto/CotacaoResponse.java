package dev.carlosheitor.agencia.preco.dto;

import java.math.BigDecimal;

import dev.carlosheitor.agencia.pagamento.FormaPagamento;

public record CotacaoResponse(
		int qtdPassageiros,
		BigDecimal valorUnitario,
		BigDecimal subtotal,
		BigDecimal percentualDesconto,
		BigDecimal valorDesconto,
		BigDecimal valorTotal,
		FormaPagamento forma,
		int parcelas,
		BigDecimal taxaParcelamento,
		BigDecimal valorCobrado,
		BigDecimal valorParcela,
		int vagasDisponiveis) {
}
