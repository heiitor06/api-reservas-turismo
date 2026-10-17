package dev.carlosheitor.agencia.preco.dto;

import dev.carlosheitor.agencia.pagamento.FormaPagamento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Limite de parcelas e "PIX só à vista" são checados na CalculadoraPreco, que lê a regra da configuração. */
public record CotacaoRequest(
		@NotNull Long saidaId,
		@NotNull @Min(1) Integer qtdPassageiros,
		@NotNull FormaPagamento forma,
		@NotNull @Min(1) Integer parcelas) {
}
