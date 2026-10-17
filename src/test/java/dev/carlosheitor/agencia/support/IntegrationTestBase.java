package dev.carlosheitor.agencia.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

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

	// Helpers de cenário: criam os dados pela própria API, como um cliente real faria

	protected long criarDestino(String nome) throws Exception {
		return idDe(mockMvc.perform(post("/api/destinos").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome": "%s", "cidade": "Barreirinhas", "uf": "MA", "pais": "Brasil"}
				""".formatted(nome)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	protected long criarPacote(long destinoId, String nome, int duracaoDias, boolean ativo) throws Exception {
		return idDe(mockMvc.perform(post("/api/pacotes").contentType(MediaType.APPLICATION_JSON).content("""
				{"destinoId": %d, "nome": "%s", "duracaoDias": %d, "precoBase": 1500.00, "ativo": %b}
				""".formatted(destinoId, nome, duracaoDias, ativo)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	protected long criarSaida(long pacoteId, String dataPartida, int capacidade) throws Exception {
		return idDe(mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"dataPartida": "%s", "capacidadeTotal": %d}
						""".formatted(dataPartida, capacidade)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	protected static long idDe(MvcResult resultado) throws Exception {
		Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
		return id.longValue();
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
