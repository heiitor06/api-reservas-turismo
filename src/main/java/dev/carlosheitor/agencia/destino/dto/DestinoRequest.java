package dev.carlosheitor.agencia.destino.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DestinoRequest(
		@NotBlank @Size(max = 120) String nome,
		@NotBlank @Size(max = 120) String cidade,
		@Size(min = 2, max = 2) String uf,
		@NotBlank @Size(max = 60) String pais,
		String descricao) {
}
