package dev.carlosheitor.agencia.saida;

import java.util.Map;

import dev.carlosheitor.agencia.compartilhado.erro.NegocioException;
import dev.carlosheitor.agencia.compartilhado.erro.TipoProblema;

public class VagasInsuficientesException extends NegocioException {

	private final int vagasDisponiveis;

	public VagasInsuficientesException(int vagasDisponiveis, int solicitadas) {
		super(TipoProblema.VAGAS_INSUFICIENTES,
				"%d vaga(s) solicitada(s), %d disponível(is)".formatted(solicitadas, vagasDisponiveis));
		this.vagasDisponiveis = vagasDisponiveis;
	}

	@Override
	public Map<String, Object> propriedades() {
		return Map.of("vagasDisponiveis", vagasDisponiveis);
	}

}
