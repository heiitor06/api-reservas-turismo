package dev.carlosheitor.agencia.reserva;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.carlosheitor.agencia.reserva.dto.CriarReservaRequest;
import dev.carlosheitor.agencia.saida.SaidaRepository;
import dev.carlosheitor.agencia.saida.VagasInsuficientesException;
import dev.carlosheitor.agencia.support.IntegrationTestBase;

/**
 * Prova a regra de overbooking com transações reais e simultâneas contra o Postgres.
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

	@Test
	void vinteThreadsDisputandoCincoVagas_criamExatamenteCincoReservas() throws Exception {
		long clienteId = criarCliente("Maria Souza", "529.982.247-25");
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		long saidaId = criarSaida(pacoteId, "2026-07-10", 5);

		int tentativas = 20;
		CountDownLatch largada = new CountDownLatch(1);
		AtomicInteger sucessos = new AtomicInteger();
		AtomicInteger semVaga = new AtomicInteger();
		List<Throwable> inesperados = new CopyOnWriteArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(tentativas)) {
			for (int i = 0; i < tentativas; i++) {
				executor.submit(() -> {
					largada.await(); // todas esperam aqui e saem juntas
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
			}
			largada.countDown();
		} // o close() do executor espera todas terminarem

		int vagasRestantes = saidaRepository.findById(saidaId).orElseThrow().getVagasDisponiveis();
		long reservasGravadas = reservaRepository.count();

		SoftAssertions.assertSoftly(soft -> {
			soft.assertThat(inesperados).as("exceções inesperadas").isEmpty();
			soft.assertThat(reservasGravadas).as("reservas gravadas no banco").isEqualTo(5);
			soft.assertThat(sucessos.get()).as("reservas aceitas").isEqualTo(5);
			soft.assertThat(semVaga.get()).as("recusadas por falta de vaga").isEqualTo(15);
			soft.assertThat(vagasRestantes).as("vagas restantes na saída").isZero();
		});
	}

}
