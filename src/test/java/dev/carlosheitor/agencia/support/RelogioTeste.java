package dev.carlosheitor.agencia.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import dev.carlosheitor.agencia.compartilhado.config.ClockConfig;

/**
 * Relógio controlável pelos testes: começa num instante fixo e só anda quando o teste manda.
 * Substitui o Clock da aplicação em IntegrationTestBase.
 */
public class RelogioTeste extends Clock {

	public static final Instant INICIO = Instant.parse("2026-06-01T12:00:00Z");

	private final AtomicReference<Instant> agora = new AtomicReference<>(INICIO);

	public void avancar(Duration duracao) {
		agora.updateAndGet(instante -> instante.plus(duracao));
	}

	public void definir(Instant instante) {
		agora.set(instante);
	}

	public void resetar() {
		agora.set(INICIO);
	}

	@Override
	public Instant instant() {
		return agora.get();
	}

	@Override
	public ZoneId getZone() {
		return ClockConfig.FUSO_AGENCIA;
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return Clock.fixed(instant(), zona);
	}

}
