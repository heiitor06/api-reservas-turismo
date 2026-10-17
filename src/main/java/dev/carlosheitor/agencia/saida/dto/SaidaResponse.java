package dev.carlosheitor.agencia.saida.dto;

import java.time.LocalDate;

import dev.carlosheitor.agencia.saida.Saida;
import dev.carlosheitor.agencia.saida.StatusSaida;

public record SaidaResponse(
		Long id,
		Long pacoteId,
		String pacoteNome,
		LocalDate dataPartida,
		LocalDate dataRetorno,
		int capacidadeTotal,
		int vagasDisponiveis,
		StatusSaida status) {

	public static SaidaResponse from(Saida saida) {
		return new SaidaResponse(saida.getId(), saida.getPacote().getId(), saida.getPacote().getNome(),
				saida.getDataPartida(), saida.dataRetorno(), saida.getCapacidadeTotal(), saida.getVagasDisponiveis(),
				saida.getStatus());
	}

}
