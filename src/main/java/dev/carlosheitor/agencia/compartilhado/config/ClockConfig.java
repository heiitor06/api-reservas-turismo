package dev.carlosheitor.agencia.compartilhado.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Agora" sempre vem deste bean, nunca de Instant.now()/LocalDate.now() direto:
 * assim os testes controlam o tempo (ex.: reservar uma saída que já partiu).
 * Instantes continuam em UTC; o fuso só decide qual é a data de "hoje" para o negócio.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	public static final ZoneId FUSO_AGENCIA = ZoneId.of("America/Sao_Paulo");

	/**
	 * Em milissegundos: o Postgres guarda timestamptz em microssegundos e arredonda o resto, então um Instant
	 * com nanossegundos voltaria diferente do banco (o POST e o GET mostrariam horários distintos).
	 */
	@Bean
	Clock clock() {
		return Clock.tickMillis(FUSO_AGENCIA);
	}

}
