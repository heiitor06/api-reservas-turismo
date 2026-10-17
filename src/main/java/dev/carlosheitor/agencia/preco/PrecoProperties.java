package dev.carlosheitor.agencia.preco;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Regras comerciais configuráveis (bloco agencia.preco do application.yml).
 * Percentuais como fração: 0.10 = 10%.
 */
@ConfigurationProperties("agencia.preco")
public record PrecoProperties(
		int minimoPassageirosGrupo,
		BigDecimal percentualDescontoGrupo,
		int parcelasSemTaxa,
		int maximoParcelas,
		BigDecimal taxaPorParcelaExtra) {
}
