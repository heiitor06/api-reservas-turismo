package dev.carlosheitor.agencia.pagamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.carlosheitor.agencia.cliente.Cliente;
import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.destino.Destino;
import dev.carlosheitor.agencia.pacote.Pacote;
import dev.carlosheitor.agencia.preco.CalculoReserva;
import dev.carlosheitor.agencia.reserva.Reserva;
import dev.carlosheitor.agencia.saida.Saida;

class PagamentoTest {

	private static final Instant AGORA = Instant.parse("2026-06-01T12:00:00Z");

	private final Reserva reserva = novaReserva();

	@Test
	void novo_comecaPendente() {
		assertThat(pagamento("1000.00").getStatus()).isEqualTo(StatusPagamento.PENDENTE);
	}

	@Test
	void aprovarPendente_ok() {
		Pagamento pagamento = pagamento("1000.00");

		pagamento.aprovar();

		assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
	}

	@Test
	void recusarPendente_ok() {
		Pagamento pagamento = pagamento("1000.00");

		pagamento.recusar();

		assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.RECUSADO);
	}

	@Test
	void aprovarAprovado_lanca() {
		Pagamento pagamento = pagamento("1000.00");
		pagamento.aprovar();

		assertThatThrownBy(pagamento::aprovar).isInstanceOf(OperacaoInvalidaException.class);
	}

	@Test
	void aprovarRecusado_lanca() {
		Pagamento pagamento = pagamento("1000.00");
		pagamento.recusar();

		assertThatThrownBy(pagamento::aprovar).isInstanceOf(OperacaoInvalidaException.class);
	}

	@Test
	void recusarRecusado_lanca() {
		Pagamento pagamento = pagamento("1000.00");
		pagamento.recusar();

		assertThatThrownBy(pagamento::recusar).isInstanceOf(OperacaoInvalidaException.class);
	}

	@Test
	void somar_consideraSoOsStatusPedidos() {
		Pagamento aprovado = pagamento("900.00");
		aprovado.aprovar();
		Pagamento pendente = pagamento("500.00");
		Pagamento recusado = pagamento("2100.00");
		recusado.recusar();
		List<Pagamento> todos = List.of(aprovado, pendente, recusado);

		assertThat(Pagamento.somar(todos, StatusPagamento.APROVADOS)).isEqualByComparingTo("900.00");
		assertThat(Pagamento.somar(todos, StatusPagamento.COMPROMETIDOS)).isEqualByComparingTo("1400.00");
		assertThat(Pagamento.somar(List.of(), StatusPagamento.COMPROMETIDOS)).isEqualByComparingTo("0");
	}

	private Pagamento pagamento(String valor) {
		BigDecimal v = new BigDecimal(valor);
		return Pagamento.novo(reserva, v, FormaPagamento.PIX, 1, BigDecimal.ZERO, v, AGORA);
	}

	private static Reserva novaReserva() {
		Destino destino = new Destino("Atins", "Barreirinhas", "MA", "Brasil", null);
		Pacote pacote = new Pacote(destino, "Atins 5 dias", null, 5, new BigDecimal("1500.00"), true);
		Saida saida = Saida.nova(pacote, LocalDate.of(2026, 12, 1), 10);
		Cliente cliente = new Cliente("Maria", "52998224725", "maria@email.com", "(98) 99999-0000");
		BigDecimal total = new BigDecimal("3000.00");
		CalculoReserva calculo = new CalculoReserva(new BigDecimal("1500.00"), total, BigDecimal.ZERO, total);
		return Reserva.criar("RSV-TESTE1", cliente, saida, 2, calculo, AGORA);
	}

}
