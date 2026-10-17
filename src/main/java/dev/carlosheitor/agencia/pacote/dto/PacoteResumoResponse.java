package dev.carlosheitor.agencia.pacote.dto;

import java.math.BigDecimal;

import dev.carlosheitor.agencia.pacote.Pacote;

/** Versão de listagem: sem os itens, para não carregar o roteiro de cada pacote da página. */
public record PacoteResumoResponse(
		Long id,
		Long destinoId,
		String destinoNome,
		String nome,
		int duracaoDias,
		BigDecimal precoBase,
		boolean ativo) {

	public static PacoteResumoResponse from(Pacote pacote) {
		return new PacoteResumoResponse(pacote.getId(), pacote.getDestino().getId(), pacote.getDestino().getNome(),
				pacote.getNome(), pacote.getDuracaoDias(), pacote.getPrecoBase(), pacote.isAtivo());
	}

}
