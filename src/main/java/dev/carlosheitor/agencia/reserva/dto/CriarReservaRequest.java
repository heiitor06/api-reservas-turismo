package dev.carlosheitor.agencia.reserva.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CriarReservaRequest(
		@NotNull Long clienteId,
		@NotNull Long saidaId,
		@NotNull @Min(1) Integer qtdPassageiros) {
}
