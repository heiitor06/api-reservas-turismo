package dev.carlosheitor.agencia.saida.dto;

import dev.carlosheitor.agencia.saida.StatusSaida;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusSaidaRequest(@NotNull StatusSaida status) {
}
