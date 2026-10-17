package dev.carlosheitor.agencia.saida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.destino.Destino;
import dev.carlosheitor.agencia.pacote.Pacote;

/** Regra de vagas testada sem Spring e sem banco: só a entidade. */
class SaidaTest {

	private static final LocalDate PARTIDA = LocalDate.of(2026, 12, 1);

	@Test
	void novaSaida_comecaAbertaComVagasIguaisACapacidade() {
		Saida saida = saida(10);

		assertThat(saida.getStatus()).isEqualTo(StatusSaida.ABERTA);
		assertThat(saida.getVagasDisponiveis()).isEqualTo(10);
	}

	@Test
	void reservarVagas_reduzVagas() {
		Saida saida = saida(10);

		saida.reservarVagas(3);

		assertThat(saida.getVagasDisponiveis()).isEqualTo(7);
	}

	@Test
	void reservarExatamenteAsVagasRestantes_zeraVagas() {
		Saida saida = saida(4);

		saida.reservarVagas(4);

		assertThat(saida.getVagasDisponiveis()).isZero();
	}

	@Test
	void reservarMaisQueDisponivel_lancaComVagasDisponiveis() {
		Saida saida = saida(2);

		assertThatThrownBy(() -> saida.reservarVagas(3))
				.isInstanceOfSatisfying(VagasInsuficientesException.class,
						ex -> assertThat(ex.propriedades()).containsEntry("vagasDisponiveis", 2));
		assertThat(saida.getVagasDisponiveis()).isEqualTo(2);
	}

	@Test
	void reservarEmSaidaEncerrada_lancaOperacaoInvalida() {
		Saida saida = saida(10);
		saida.alterarStatus(StatusSaida.ENCERRADA);

		assertThatThrownBy(() -> saida.reservarVagas(1)).isInstanceOf(OperacaoInvalidaException.class);
		assertThat(saida.getVagasDisponiveis()).isEqualTo(10);
	}

	@Test
	void reservarQuantidadeZero_lancaIllegalArgument() {
		Saida saida = saida(10);

		assertThatThrownBy(() -> saida.reservarVagas(0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void devolverVagas_aumentaVagas() {
		Saida saida = saida(10);
		saida.reservarVagas(5);

		saida.devolverVagas(2);

		assertThat(saida.getVagasDisponiveis()).isEqualTo(7);
	}

	@Test
	void devolverAlemDaCapacidade_lancaIllegalState() {
		Saida saida = saida(10);
		saida.reservarVagas(1);

		assertThatThrownBy(() -> saida.devolverVagas(2)).isInstanceOf(IllegalStateException.class);
		assertThat(saida.getVagasDisponiveis()).isEqualTo(9);
	}

	@Test
	void encerrarEReabrir_ok() {
		Saida saida = saida(10);

		saida.alterarStatus(StatusSaida.ENCERRADA);
		saida.alterarStatus(StatusSaida.ABERTA);

		assertThat(saida.getStatus()).isEqualTo(StatusSaida.ABERTA);
	}

	@Test
	void cancelarSemReservas_ok() {
		Saida saida = saida(10);

		saida.alterarStatus(StatusSaida.CANCELADA);

		assertThat(saida.getStatus()).isEqualTo(StatusSaida.CANCELADA);
	}

	@Test
	void cancelarComVagasOcupadas_lancaOperacaoInvalida() {
		Saida saida = saida(10);
		saida.reservarVagas(1);

		assertThatThrownBy(() -> saida.alterarStatus(StatusSaida.CANCELADA))
				.isInstanceOf(OperacaoInvalidaException.class);
		assertThat(saida.getStatus()).isEqualTo(StatusSaida.ABERTA);
	}

	@Test
	void cancelada_naoVoltaParaAberta() {
		Saida saida = saida(10);
		saida.alterarStatus(StatusSaida.CANCELADA);

		assertThatThrownBy(() -> saida.alterarStatus(StatusSaida.ABERTA))
				.isInstanceOf(OperacaoInvalidaException.class);
	}

	@Test
	void dataRetorno_consideraDuracao() {
		assertThat(saida(10).dataRetorno()).isEqualTo(LocalDate.of(2026, 12, 5));
	}

	@Test
	void jaPartiu_contaODiaDaPartida() {
		Saida saida = saida(10);

		assertThat(saida.jaPartiu(PARTIDA.minusDays(1))).isFalse();
		assertThat(saida.jaPartiu(PARTIDA)).isTrue();
		assertThat(saida.jaPartiu(PARTIDA.plusDays(1))).isTrue();
	}

	private static Saida saida(int capacidade) {
		Destino destino = new Destino("Atins", "Barreirinhas", "MA", "Brasil", null);
		Pacote pacote = new Pacote(destino, "Atins 5 dias", null, 5, new BigDecimal("1500.00"), true);
		return Saida.nova(pacote, PARTIDA, capacidade);
	}

}
