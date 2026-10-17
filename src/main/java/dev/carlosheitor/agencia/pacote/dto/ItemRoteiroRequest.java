package dev.carlosheitor.agencia.pacote.dto;

import dev.carlosheitor.agencia.pacote.TipoItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ItemRoteiroRequest(
		@NotNull TipoItem tipo,
		@NotBlank @Size(max = 150) String titulo,
		String descricao,
		@NotNull @Positive Integer dia,
		@NotNull @PositiveOrZero Integer ordem) {
}
