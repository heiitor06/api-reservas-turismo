package dev.carlosheitor.agencia.saida.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaidaRequest(@NotNull LocalDate dataPartida, @NotNull @Positive Integer capacidadeTotal) {
}
