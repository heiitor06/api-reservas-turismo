package dev.carlosheitor.agencia.pagamento.dto;

import java.math.BigDecimal;
import java.time.Instant;

import dev.carlosheitor.agencia.pagamento.FormaPagamento;
import dev.carlosheitor.agencia.pagamento.Pagamento;
import dev.carlosheitor.agencia.pagamento.StatusPagamento;
import dev.carlosheitor.agencia.reserva.StatusReserva;

/** Traz o status da reserva: quem aprova um pagamento já fica sabendo se ele confirmou a reserva. */
public record PagamentoResponse(
		Long id,
		String reservaCodigo,
		StatusReserva reservaStatus,
		BigDecimal valor,
		FormaPagamento forma,
		int parcelas,
		BigDecimal taxaPercentual,
		BigDecimal valorCobrado,
		StatusPagamento status,
		Instant criadoEm) {

	public static PagamentoResponse from(Pagamento pagamento) {
		return new PagamentoResponse(pagamento.getId(), pagamento.getReserva().getCodigo(),
				pagamento.getReserva().getStatus(), pagamento.getValor(), pagamento.getForma(), pagamento.getParcelas(),
				pagamento.getTaxaPercentual(), pagamento.getValorCobrado(), pagamento.getStatus(),
				pagamento.getCriadoEm());
	}

}
