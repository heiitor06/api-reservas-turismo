package dev.carlosheitor.agencia.reserva.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import dev.carlosheitor.agencia.reserva.Reserva;
import dev.carlosheitor.agencia.reserva.StatusReserva;

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
		StatusReserva status,
		Instant criadaEm,
		Instant canceladaEm) {

	public static ReservaResponse from(Reserva reserva) {
		var saida = reserva.getSaida();
		return new ReservaResponse(reserva.getCodigo(), reserva.getCliente().getId(), reserva.getCliente().getNome(),
				saida.getId(), saida.getPacote().getNome(), saida.getDataPartida(), saida.dataRetorno(),
				reserva.getQtdPassageiros(), reserva.getValorUnitario(), reserva.getPercentualDesconto(),
				reserva.getValorTotal(), reserva.getStatus(), reserva.getCriadaEm(), reserva.getCanceladaEm());
	}

}
