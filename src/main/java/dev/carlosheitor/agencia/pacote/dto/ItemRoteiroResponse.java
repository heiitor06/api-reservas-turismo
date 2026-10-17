package dev.carlosheitor.agencia.pacote.dto;

import dev.carlosheitor.agencia.pacote.ItemRoteiro;
import dev.carlosheitor.agencia.pacote.TipoItem;

public record ItemRoteiroResponse(Long id, TipoItem tipo, String titulo, String descricao, int dia, int ordem) {

	public static ItemRoteiroResponse from(ItemRoteiro item) {
		return new ItemRoteiroResponse(item.getId(), item.getTipo(), item.getTitulo(), item.getDescricao(),
				item.getDia(), item.getOrdem());
	}

}
