package dev.carlosheitor.agencia.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.carlosheitor.agencia.cliente.Cliente;
import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.destino.Destino;
import dev.carlosheitor.agencia.pacote.Pacote;
import dev.carlosheitor.agencia.preco.CalculoReserva;
import dev.carlosheitor.agencia.saida.Saida;
import dev.carlosheitor.agencia.saida.VagasInsuficientesException;

class ReservaTest {

	private static final Instant AGORA = Instant.parse("2026-06-01T12:00:00Z");

	private final Saida saida = novaSaida(10);

	@Test
	void criar_debitaVagasDaSaidaEComecaPendente() {
		Reserva reserva = reserva(3);

		assertThat(reserva.getStatus()).isEqualTo(StatusReserva.PENDENTE);
		assertThat(reserva.getCriadaEm()).isEqualTo(AGORA);
		assertThat(reserva.getValorTotal()).isEqualByComparingTo("4500.00");
		assertThat(saida.getVagasDisponiveis()).isEqualTo(7);
	}

	@Test
	void criarSemVagas_lancaENaoDebita() {
		assertThatThrownBy(() -> reserva(11)).isInstanceOf(VagasInsuficientesException.class);
		assertThat(saida.getVagasDisponiveis()).isEqualTo(10);
	}

	@Test
	void confirmarPendente_ok() {
		Reserva reserva = reserva(2);

		reserva.confirmar();

		assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
	}

	@Test
	void confirmarConfirmada_lanca() {
		Reserva reserva = reserva(2);
		reserva.confirmar();

		assertThatThrownBy(reserva::confirmar).isInstanceOf(OperacaoInvalidaException.class);
	}

	@Test
	void confirmarCancelada_lanca() {
		Reserva reserva = reserva(2);
		reserva.cancelar(AGORA);

		assertThatThrownBy(reserva::confirmar).isInstanceOf(OperacaoInvalidaException.class);
	}

	@Test
	void cancelarPendente_devolveVagasEMarcaData() {
		Reserva reserva = reserva(3);
		Instant depois = AGORA.plusSeconds(3600);

		reserva.cancelar(depois);

		assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CANCELADA);
		assertThat(reserva.getCanceladaEm()).isEqualTo(depois);
		assertThat(saida.getVagasDisponiveis()).isEqualTo(10);
	}

	@Test
	void cancelarConfirmada_devolveVagas() {
		Reserva reserva = reserva(3);
		reserva.confirmar();

		reserva.cancelar(AGORA);

		assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CANCELADA);
		assertThat(saida.getVagasDisponiveis()).isEqualTo(10);
	}

	@Test
	void cancelarCancelada_lancaENaoDevolveDeNovo() {
		Reserva reserva = reserva(3);
		reserva(2);
		reserva.cancelar(AGORA);

		assertThatThrownBy(() -> reserva.cancelar(AGORA)).isInstanceOf(OperacaoInvalidaException.class);
		assertThat(saida.getVagasDisponiveis()).isEqualTo(8);
	}

	private Reserva reserva(int qtd) {
		Cliente cliente = new Cliente("Maria", "52998224725", "maria@email.com", "(98) 99999-0000");
		BigDecimal total = new BigDecimal("1500.00").multiply(BigDecimal.valueOf(qtd));
		CalculoReserva calculo = new CalculoReserva(new BigDecimal("1500.00"), total, BigDecimal.ZERO, total);
		return Reserva.criar("RSV-TESTE1", cliente, saida, qtd, calculo, AGORA);
	}

	private static Saida novaSaida(int capacidade) {
		Destino destino = new Destino("Atins", "Barreirinhas", "MA", "Brasil", null);
		Pacote pacote = new Pacote(destino, "Atins 5 dias", null, 5, new BigDecimal("1500.00"), true);
		return Saida.nova(pacote, LocalDate.of(2026, 12, 1), capacidade);
	}

}
