package dev.carlosheitor.agencia.reserva.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import dev.carlosheitor.agencia.pagamento.Pagamento;
import dev.carlosheitor.agencia.pagamento.StatusPagamento;
import dev.carlosheitor.agencia.pagamento.dto.PagamentoResponse;
import dev.carlosheitor.agencia.reserva.Reserva;
import dev.carlosheitor.agencia.reserva.StatusReserva;

/**
 * valorPago: soma dos pagamentos aprovados.
 * saldo: quanto ainda pode ser pago (total − aprovados − pendentes); zero se a reserva não está pendente.
 */
public record ReservaResponse(
		String codigo,
		Long clienteId,
		String clienteNome,
		Long saidaId,
		String pacoteNome,
		LocalDate dataPartida,
		LocalDate dataRetorno,
		int qtdPassageiros,
		BigDecimal valorUnitario,
		BigDecimal percentualDesconto,
		BigDecimal valorTotal,
		BigDecimal valorPago,
		BigDecimal saldo,
		StatusReserva status,
		Instant criadaEm,
		Instant canceladaEm,
		List<PagamentoResponse> pagamentos) {

	public static ReservaResponse from(Reserva reserva, List<Pagamento> pagamentos) {
		var saida = reserva.getSaida();
		BigDecimal saldo = reserva.getStatus() == StatusReserva.PENDENTE
				? reserva.getValorTotal().subtract(Pagamento.somar(pagamentos, StatusPagamento.COMPROMETIDOS))
				: BigDecimal.ZERO;
		return new ReservaResponse(reserva.getCodigo(), reserva.getCliente().getId(), reserva.getCliente().getNome(),
				saida.getId(), saida.getPacote().getNome(), saida.getDataPartida(), saida.dataRetorno(),
				reserva.getQtdPassageiros(), reserva.getValorUnitario(), reserva.getPercentualDesconto(),
				reserva.getValorTotal(), Pagamento.somar(pagamentos, StatusPagamento.APROVADOS), saldo,
				reserva.getStatus(), reserva.getCriadaEm(), reserva.getCanceladaEm(),
				pagamentos.stream().map(PagamentoResponse::from).toList());
	}

}
