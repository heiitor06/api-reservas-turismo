package dev.carlosheitor.agencia.preco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dev.carlosheitor.agencia.pagamento.FormaPagamento;

/** Regras de preço com os valores de exemplo do application.yml, instanciadas na mão (sem Spring). */
class CalculadoraPrecoTest {

	private final CalculadoraPreco calculadora = new CalculadoraPreco(
			new PrecoProperties(6, new BigDecimal("0.10"), 3, 10, new BigDecimal("0.015")));

	@ParameterizedTest(name = "{0} x {1} passageiros = {4}")
	@CsvSource({
			// precoBase, qtd, subtotal, percentualDesconto, valorTotal
			"1500.00, 2, 3000.00, 0,    3000.00",
			"1500.00, 5, 7500.00, 0,    7500.00", // 5 ainda não é grupo
			"1500.00, 6, 9000.00, 0.10, 8100.00", // a partir de 6: 10%
			"999.99,  7, 6999.93, 0.10, 6299.94", // 6299.937 arredonda para 6299.94
	})
	void calcularReserva(String precoBase, int qtd, String subtotal, String percentual, String total) {
		CalculoReserva calculo = calculadora.calcularReserva(new BigDecimal(precoBase), qtd);

		assertThat(calculo.valorUnitario()).isEqualByComparingTo(precoBase);
		assertThat(calculo.subtotal()).isEqualByComparingTo(subtotal);
		assertThat(calculo.percentualDesconto()).isEqualByComparingTo(percentual);
		assertThat(calculo.valorTotal()).isEqualByComparingTo(total);
		assertThat(calculo.valorTotal().scale()).isEqualTo(2);
	}

	@ParameterizedTest(name = "{0} em {1}x = taxa {2}")
	@CsvSource({
			"PIX,    1,  0",
			"BOLETO, 1,  0",
			"CARTAO, 1,  0",
			"CARTAO, 3,  0",
			"CARTAO, 4,  0.015",
			"CARTAO, 6,  0.045",
			"CARTAO, 10, 0.105",
	})
	void taxaParcelamento(FormaPagamento forma, int parcelas, String taxa) {
		assertThat(calculadora.taxaParcelamento(forma, parcelas)).isEqualByComparingTo(taxa);
	}

	@Test
	void pixParcelado_lancaParcelamentoInvalido() {
		assertThatThrownBy(() -> calculadora.taxaParcelamento(FormaPagamento.PIX, 3))
				.isInstanceOf(ParcelamentoInvalidoException.class);
	}

	@Test
	void cartaoAcimaDoMaximo_lancaParcelamentoInvalido() {
		assertThatThrownBy(() -> calculadora.taxaParcelamento(FormaPagamento.CARTAO, 11))
				.isInstanceOf(ParcelamentoInvalidoException.class)
				.hasMessageContaining("10");
	}

	@Test
	void zeroParcelas_lancaParcelamentoInvalido() {
		assertThatThrownBy(() -> calculadora.taxaParcelamento(FormaPagamento.CARTAO, 0))
				.isInstanceOf(ParcelamentoInvalidoException.class);
	}

	@Test
	void valorCobrado_aplicaTaxa() {
		assertThat(calculadora.valorCobrado(new BigDecimal("3000.00"), new BigDecimal("0.045")))
				.isEqualByComparingTo("3135.00");
	}

	@ParameterizedTest(name = "{0} em {1}x = {2} por parcela")
	@CsvSource({
			"8464.50, 6, 1410.75",
			"1000.00, 3, 333.33",
	})
	void valorParcela(String valorCobrado, int parcelas, String esperado) {
		assertThat(calculadora.valorParcela(new BigDecimal(valorCobrado), parcelas)).isEqualByComparingTo(esperado);
	}

	@Test
	void regrasVemDaConfiguracao() {
		CalculadoraPreco promocao = new CalculadoraPreco(
				new PrecoProperties(4, new BigDecimal("0.05"), 6, 12, new BigDecimal("0.02")));

		assertThat(promocao.calcularReserva(new BigDecimal("1000.00"), 4).valorTotal()).isEqualByComparingTo("3800.00");
		assertThat(promocao.taxaParcelamento(FormaPagamento.CARTAO, 6)).isEqualByComparingTo("0");
		assertThat(promocao.taxaParcelamento(FormaPagamento.CARTAO, 12)).isEqualByComparingTo("0.12");
	}

}
