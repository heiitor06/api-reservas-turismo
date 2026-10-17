package dev.carlosheitor.agencia.reserva;

import static dev.carlosheitor.agencia.support.Concorrencia.executarJuntos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.reserva.dto.CriarReservaRequest;
import dev.carlosheitor.agencia.saida.SaidaRepository;
import dev.carlosheitor.agencia.saida.VagasInsuficientesException;
import dev.carlosheitor.agencia.support.IntegrationTestBase;

/**
 * Prova as regras de vaga com transações reais e simultâneas contra o Postgres.
 * Chama o service direto (sem HTTP) para a disputa acontecer só onde interessa: na linha da saída.
 * Sem @Transactional no teste: cada thread abre e confirma a sua própria transação, como em produção.
 */
class ConcorrenciaReservaIT extends IntegrationTestBase {

	@Autowired
	private ReservaService reservaService;

	@Autowired
	private ReservaRepository reservaRepository;

	@Autowired
	private SaidaRepository saidaRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private long clienteId;
	private long pacoteId;

	@BeforeEach
	void cenario() throws Exception {
		clienteId = criarCliente("Maria Souza", "529.982.247-25");
		pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
	}

	@Test
	void vinteThreadsDisputandoCincoVagas_criamExatamenteCincoReservas() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 5);
		AtomicInteger sucessos = new AtomicInteger();
		AtomicInteger semVaga = new AtomicInteger();
		List<Throwable> inesperados = new CopyOnWriteArrayList<>();

		executarJuntos(20, i -> () -> {
			try {
				reservaService.criar(new CriarReservaRequest(clienteId, saidaId, 1));
				sucessos.incrementAndGet();
			} catch (VagasInsuficientesException e) {
				semVaga.incrementAndGet();
			} catch (Exception e) {
				inesperados.add(e);
			}
			return null;
		});

		int vagasRestantes = vagas(saidaId);
		long reservasGravadas = reservaRepository.count();

		SoftAssertions.assertSoftly(soft -> {
			soft.assertThat(inesperados).as("exceções inesperadas").isEmpty();
			soft.assertThat(reservasGravadas).as("reservas gravadas no banco").isEqualTo(5);
			soft.assertThat(sucessos.get()).as("reservas aceitas").isEqualTo(5);
			soft.assertThat(semVaga.get()).as("recusadas por falta de vaga").isEqualTo(15);
			soft.assertThat(vagasRestantes).as("vagas restantes na saída").isZero();
		});
	}

	/**
	 * Cancelamentos devolvem vagas enquanto reservas novas consomem. Qualquer que seja a ordem,
	 * o saldo tem que fechar: vagas livres = capacidade − passageiros em reservas ativas.
	 */
	@Test
	void reservasECancelamentosSimultaneos_fechamOSaldo() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		List<String> paraCancelar = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			paraCancelar.add(reservaService.criar(new CriarReservaRequest(clienteId, saidaId, 1)).codigo());
		}
		AtomicInteger cancelamentos = new AtomicInteger();
		AtomicInteger novas = new AtomicInteger();
		AtomicInteger semVaga = new AtomicInteger();
		List<Throwable> inesperados = new CopyOnWriteArrayList<>();

		executarJuntos(15, i -> () -> {
			try {
				if (i < 5) {
					reservaService.cancelar(paraCancelar.get(i));
					cancelamentos.incrementAndGet();
				} else {
					reservaService.criar(new CriarReservaRequest(clienteId, saidaId, 1));
					novas.incrementAndGet();
				}
			} catch (VagasInsuficientesException e) {
				semVaga.incrementAndGet();
			} catch (Exception e) {
				inesperados.add(e);
			}
			return null;
		});

		long passageirosAtivos = reservaRepository.findAll().stream()
				.filter(r -> r.getStatus() != StatusReserva.CANCELADA)
				.mapToInt(Reserva::getQtdPassageiros)
				.sum();
		int vagasRestantes = vagas(saidaId);

		SoftAssertions.assertSoftly(soft -> {
			soft.assertThat(inesperados).as("exceções inesperadas").isEmpty();
			soft.assertThat(cancelamentos.get()).as("cancelamentos feitos").isEqualTo(5);
			soft.assertThat(novas.get() + semVaga.get()).as("tentativas de reserva respondidas").isEqualTo(10);
			soft.assertThat(vagasRestantes).as("vagas = capacidade − passageiros ativos")
					.isEqualTo(10 - passageirosAtivos);
		});
	}

	/** Review Focus 1: clique duplo no "cancelar" não pode devolver as vagas duas vezes. */
	@Test
	void doisCancelamentosSimultaneosDaMesmaReserva_devolvemVagasUmaVez() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		String codigo = reservaService.criar(new CriarReservaRequest(clienteId, saidaId, 3)).codigo();
		reservaService.criar(new CriarReservaRequest(clienteId, saidaId, 2));
		AtomicInteger sucessos = new AtomicInteger();
		AtomicInteger jaCancelada = new AtomicInteger();
		List<Throwable> inesperados = new CopyOnWriteArrayList<>();

		executarJuntos(2, i -> () -> {
			try {
				reservaService.cancelar(codigo);
				sucessos.incrementAndGet();
			} catch (OperacaoInvalidaException e) {
				jaCancelada.incrementAndGet();
			} catch (Exception e) {
				inesperados.add(e);
			}
			return null;
		});

		int vagasRestantes = vagas(saidaId);

		SoftAssertions.assertSoftly(soft -> {
			soft.assertThat(inesperados).as("exceções inesperadas").isEmpty();
			soft.assertThat(sucessos.get()).as("cancelamentos aceitos").isEqualTo(1);
			soft.assertThat(jaCancelada.get()).as("recusados por já estar cancelada").isEqualTo(1);
			soft.assertThat(vagasRestantes).as("vagas (10 − 2 da outra reserva)").isEqualTo(8);
		});
	}

	/**
	 * Uma transação segura o lock da saída por mais tempo que o timeout. A reserva que chega
	 * não pode ficar presa: desiste em ~3 s e responde 503 com Retry-After.
	 */
	@Test
	void lockOcupadoAlemDoTimeout_retorna503() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		CountDownLatch travou = new CountDownLatch(1);
		CountDownLatch liberar = new CountDownLatch(1);
		TransactionTemplate transacao = new TransactionTemplate(transactionManager);

		try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
			executor.submit(() -> transacao.executeWithoutResult(tx -> {
				saidaRepository.findByIdForUpdate(saidaId);
				travou.countDown();
				aguardar(liberar, 10);
			}));
			assertThat(travou.await(5, TimeUnit.SECONDS)).as("a outra transação travou a saída").isTrue();

			long inicio = System.nanoTime();
			mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("""
					{"clienteId": %d, "saidaId": %d, "qtdPassageiros": 1}
					""".formatted(clienteId, saidaId)))
					.andExpect(status().isServiceUnavailable())
					.andExpect(header().string("Retry-After", "1"))
					.andExpect(jsonPath("$.type").value("https://api.agencia.local/problemas/tente-novamente"));
			long esperouMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

			assertThat(esperouMs).as("tempo até desistir do lock (ms)").isBetween(2_500L, 6_000L);
		} finally {
			liberar.countDown();
		}
		assertThat(vagas(saidaId)).isEqualTo(10);
	}

	private int vagas(long saidaId) {
		return saidaRepository.findById(saidaId).orElseThrow().getVagasDisponiveis();
	}

	private static void aguardar(CountDownLatch latch, int segundos) {
		try {
			latch.await(segundos, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
