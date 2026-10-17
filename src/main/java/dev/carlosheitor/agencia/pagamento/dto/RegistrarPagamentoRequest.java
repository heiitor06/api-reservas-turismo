package dev.carlosheitor.agencia.pagamento.dto;

import java.math.BigDecimal;

import dev.carlosheitor.agencia.pagamento.FormaPagamento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Limite de parcelas e "PIX/boleto só à vista" são checados na CalculadoraPreco, como na cotação. */
public record RegistrarPagamentoRequest(
		@NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal valor,
		@NotNull FormaPagamento forma,
		@NotNull @Min(1) Integer parcelas) {
}
