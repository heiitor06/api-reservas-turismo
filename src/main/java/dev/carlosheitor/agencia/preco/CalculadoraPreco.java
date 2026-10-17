package dev.carlosheitor.agencia.preco;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import dev.carlosheitor.agencia.pagamento.FormaPagamento;

/**
 * Todo cálculo de dinheiro da aplicação passa por aqui. Classe pura (só depende das properties),
 * então as regras são testadas sem Spring.
 * Arredondamento HALF_EVEN (o "do banqueiro") só no valor final, nunca em passos intermediários.
 */
@Component
public class CalculadoraPreco {

	private static final int CASAS = 2;
	private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

	private final PrecoProperties regras;

	public CalculadoraPreco(PrecoProperties regras) {
		this.regras = regras;
	}

	public CalculoReserva calcularReserva(BigDecimal precoBase, int qtdPassageiros) {
		BigDecimal subtotal = precoBase.multiply(BigDecimal.valueOf(qtdPassageiros));
		BigDecimal percentual = qtdPassageiros >= regras.minimoPassageirosGrupo()
				? regras.percentualDescontoGrupo()
				: BigDecimal.ZERO;
		BigDecimal total = subtotal.multiply(BigDecimal.ONE.subtract(percentual)).setScale(CASAS, ARREDONDAMENTO);
		return new CalculoReserva(precoBase, subtotal, percentual, total);
	}

	/** Fração sobre o valor pago: 0.045 = 4,5%. PIX e boleto são sempre à vista. */
	public BigDecimal taxaParcelamento(FormaPagamento forma, int parcelas) {
		if (parcelas < 1 || parcelas > regras.maximoParcelas()) {
			throw new ParcelamentoInvalidoException(
					"Parcelas devem estar entre 1 e %d: %d".formatted(regras.maximoParcelas(), parcelas));
		}
		if (forma != FormaPagamento.CARTAO) {
			if (parcelas != 1) {
				throw new ParcelamentoInvalidoException("%s só aceita pagamento à vista".formatted(forma));
			}
			return BigDecimal.ZERO;
		}
		int parcelasComTaxa = Math.max(0, parcelas - regras.parcelasSemTaxa());
		return regras.taxaPorParcelaExtra().multiply(BigDecimal.valueOf(parcelasComTaxa));
	}

	public BigDecimal valorCobrado(BigDecimal valor, BigDecimal taxa) {
		return valor.multiply(BigDecimal.ONE.add(taxa)).setScale(CASAS, ARREDONDAMENTO);
	}

	/** Informativo: a soma das parcelas arredondadas pode diferir do total em centavos. */
	public BigDecimal valorParcela(BigDecimal valorCobrado, int parcelas) {
		return valorCobrado.divide(BigDecimal.valueOf(parcelas), CASAS, ARREDONDAMENTO);
	}

}
