package dev.carlosheitor.agencia.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import dev.carlosheitor.agencia.TestcontainersConfiguration;

/**
 * Base dos testes de integração. Toda classe que estende esta usa a mesma configuração,
 * então o Spring reaproveita o contexto e o container do Postgres sobe uma vez só.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, IntegrationTestBase.Config.class })
public abstract class IntegrationTestBase {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected RelogioTeste relogio;

	@Autowired
	private LimpezaBanco limpezaBanco;

	@BeforeEach
	void prepararAmbiente() {
		limpezaBanco.limpar();
		relogio.resetar();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class Config {

		@Bean
		@Primary
		RelogioTeste relogioTeste() {
			return new RelogioTeste();
		}

		@Bean
		LimpezaBanco limpezaBanco(JdbcTemplate jdbc) {
			return new LimpezaBanco(jdbc);
		}

	}

}
