package dev.carlosheitor.agencia.compartilhado.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Agora" sempre vem deste bean, nunca de Instant.now()/LocalDate.now() direto:
 * assim os testes controlam o tempo (ex.: reservar uma saída que já partiu).
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
