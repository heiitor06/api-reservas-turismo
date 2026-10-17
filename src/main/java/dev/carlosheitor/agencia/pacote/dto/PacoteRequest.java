package dev.carlosheitor.agencia.pacote.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PacoteRequest(
		@NotNull Long destinoId,
		@NotBlank @Size(max = 150) String nome,
		String descricao,
		@NotNull @Positive Integer duracaoDias,
		@NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal precoBase,
		@NotNull Boolean ativo) {
}
