package dev.carlosheitor.agencia.pacote.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import dev.carlosheitor.agencia.pacote.Pacote;

public record PacoteResponse(
		Long id,
		Long destinoId,
		String destinoNome,
		String nome,
		String descricao,
		int duracaoDias,
		BigDecimal precoBase,
		boolean ativo,
		List<ItemRoteiroResponse> itens) {

	private static final Comparator<ItemRoteiroResponse> ORDEM_DO_ROTEIRO = Comparator
			.comparingInt(ItemRoteiroResponse::dia)
			.thenComparingInt(ItemRoteiroResponse::ordem);

	public static PacoteResponse from(Pacote pacote) {
		List<ItemRoteiroResponse> itens = pacote.getItens().stream()
				.map(ItemRoteiroResponse::from)
				.sorted(ORDEM_DO_ROTEIRO)
				.toList();
		return new PacoteResponse(pacote.getId(), pacote.getDestino().getId(), pacote.getDestino().getNome(),
				pacote.getNome(), pacote.getDescricao(), pacote.getDuracaoDias(), pacote.getPrecoBase(),
				pacote.isAtivo(), itens);
	}

}
