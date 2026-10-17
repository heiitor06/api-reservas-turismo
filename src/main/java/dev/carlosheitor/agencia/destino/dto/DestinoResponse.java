package dev.carlosheitor.agencia.destino.dto;

import dev.carlosheitor.agencia.destino.Destino;

public record DestinoResponse(Long id, String nome, String cidade, String uf, String pais, String descricao) {

	public static DestinoResponse from(Destino destino) {
		return new DestinoResponse(destino.getId(), destino.getNome(), destino.getCidade(),
				destino.getUf(), destino.getPais(), destino.getDescricao());
	}

}
