package dev.carlosheitor.agencia.pagamento;

import static dev.carlosheitor.agencia.support.Concorrencia.executarJuntos;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.pagamento.dto.RegistrarPagamentoRequest;
import dev.carlosheitor.agencia.reserva.ReservaRepository;
import dev.carlosheitor.agencia.reserva.StatusReserva;
import dev.carlosheitor.agencia.support.IntegrationTestBase;

/** Reserva base: 2 passageiros × 1500,00 = 3000,00. Sem @Transactional, como em ConcorrenciaReservaIT. */
class ConcorrenciaPagamentoIT extends IntegrationTestBase {

	@Autowired
	private PagamentoService pagamentoService;

	@Autowired
	private PagamentoRepository pagamentoRepository;

	@Autowired
	private ReservaRepository reservaRepository;

	private String codigo;

	@BeforeEach
	void cenario() throws Exception {
		long clienteId = criarCliente("Maria Souza", "529.982.247-25");
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		codigo = criarReserva(clienteId, criarSaida(pacoteId, "2026-07-10", 10), 2);
	}

	/**
	 * Dois pagamentos de metade aprovados ao mesmo tempo. Sem lock, cada transação só enxerga a própria
	 * aprovação (1500 de 3000) e nenhuma confirma: a reserva fica paga e PENDENTE para sempre.
	 * A janela da corrida é curta (entre ler os pagamentos e confirmar), por isso o teste repete.
	 */
	@RepeatedTest(20)
	void duasAprovacoesSimultaneasDePagamentosDiferentes_confirmamAReserva() throws Exception {
		long metade1 = registrar("1500.00");
		long metade2 = registrar("1500.00");
		List<Long> pagamentos = List.of(metade1, metade2);
		List<Throwable> inesperados = new CopyOnWriteArrayList<>();

		executarJuntos(2, i -> () -> {
			try {
				pagamentoService.aprovar(pagamentos.get(i));
			} catch (Exception e) {
				inesperados.add(e);
			}
			return null;
		});

		StatusReserva statusReserva = reservaRepository.findByCodigo(codigo).orElseThrow().getStatus();
		long aprovados = pagamentoRepository.findAll().stream()
				.filter(p -> p.getStatus() == StatusPagamento.APROVADO).count();

		SoftAssertions.assertSoftly(soft -> {
			soft.assertThat(inesperados).as("exceções inesperadas").isEmpty();
			soft.assertThat(aprovados).as("pagamentos aprovados").isEqualTo(2);
			soft.assertThat(statusReserva).as("status da reserva").isEqualTo(StatusReserva.CONFIRMADA);
		});
	}

	/** O mesmo webhook de aprovação chegando duas vezes ao mesmo tempo. */
	@Test
	void mesmaAprovacaoEmParalelo_umSucessoUmConflito() throws Exception {
		long pagamentoId = registrar("3000.00");
		AtomicInteger sucessos = new AtomicInteger();
		AtomicInteger conflitos = new AtomicInteger();
		List<Throwable> inesperados = new CopyOnWriteArrayList<>();

		executarJuntos(2, i -> () -> {
			try {
				pagamentoService.aprovar(pagamentoId);
				sucessos.incrementAndGet();
			} catch (OperacaoInvalidaException e) {
				conflitos.incrementAndGet();
			} catch (Exception e) {
				inesperados.add(e);
			}
			return null;
		});

		StatusReserva statusReserva = reservaRepository.findByCodigo(codigo).orElseThrow().getStatus();

		SoftAssertions.assertSoftly(soft -> {
			soft.assertThat(inesperados).as("exceções inesperadas").isEmpty();
			soft.assertThat(sucessos.get()).as("aprovações aceitas").isEqualTo(1);
			soft.assertThat(conflitos.get()).as("aprovações recusadas por já estar aprovado").isEqualTo(1);
			soft.assertThat(statusReserva).as("status da reserva").isEqualTo(StatusReserva.CONFIRMADA);
		});
	}

	/** Review Focus 2: dois pagamentos do valor total ao mesmo tempo; só cabe um. */
	@Test
	void doisPagamentosSimultaneosAcimaDoSaldo_soUmAceito() throws Exception {
		AtomicInteger aceitos = new AtomicInteger();
		AtomicInteger saldoExcedido = new AtomicInteger();
		List<Throwable> inesperados = new CopyOnWriteArrayList<>();

		executarJuntos(2, i -> () -> {
			try {
				pagamentoService.registrar(codigo, new RegistrarPagamentoRequest(new BigDecimal("3000.00"),
						FormaPagamento.PIX, 1));
				aceitos.incrementAndGet();
			} catch (SaldoExcedidoException e) {
				saldoExcedido.incrementAndGet();
			} catch (Exception e) {
				inesperados.add(e);
			}
			return null;
		});

		long gravados = pagamentoRepository.count();

		SoftAssertions.assertSoftly(soft -> {
			soft.assertThat(inesperados).as("exceções inesperadas").isEmpty();
			soft.assertThat(aceitos.get()).as("pagamentos aceitos").isEqualTo(1);
			soft.assertThat(saldoExcedido.get()).as("recusados por saldo").isEqualTo(1);
			soft.assertThat(gravados).as("pagamentos gravados").isEqualTo(1);
		});
	}

	private long registrar(String valor) {
		return pagamentoService.registrar(codigo,
				new RegistrarPagamentoRequest(new BigDecimal(valor), FormaPagamento.PIX, 1)).id();
	}

}
