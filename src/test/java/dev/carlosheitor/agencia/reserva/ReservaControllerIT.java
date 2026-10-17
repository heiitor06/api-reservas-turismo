package dev.carlosheitor.agencia.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import dev.carlosheitor.agencia.support.IntegrationTestBase;

class ReservaControllerIT extends IntegrationTestBase {

	private static final String TIPO = "https://api.agencia.local/problemas/";

	private long destinoId;
	private long pacoteId;
	private long clienteId;

	@BeforeEach
	void cenario() throws Exception {
		destinoId = criarDestino("Atins");
		pacoteId = criarPacote(destinoId, "Atins 5 dias", 5, true);
		clienteId = criarCliente("Maria Souza", "529.982.247-25");
	}

	@Test
	void criar_debitaVagasEGravaPreco() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);

		MvcResult resultado = mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(clienteId, saidaId, 2)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value(matchesPattern("^RSV-[A-HJKMNP-Z2-9]{6}$")))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.clienteNome").value("Maria Souza"))
				.andExpect(jsonPath("$.pacoteNome").value("Atins 5 dias"))
				.andExpect(jsonPath("$.dataPartida").value("2026-07-10"))
				.andExpect(jsonPath("$.dataRetorno").value("2026-07-14"))
				.andExpect(jsonPath("$.qtdPassageiros").value(2))
				.andExpect(jsonPath("$.valorUnitario").value(1500.0))
				.andExpect(jsonPath("$.valorTotal").value(3000.0))
				.andExpect(jsonPath("$.criadaEm").value("2026-06-01T12:00:00Z"))
				.andReturn();

		String codigo = JsonPath.read(resultado.getResponse().getContentAsString(), "$.codigo");
		mockMvc.perform(get("/api/reservas/" + codigo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value(codigo));
		assertVagas(saidaId, 8);
		assertThat(resultado.getResponse().getHeader("Location")).endsWith("/api/reservas/" + codigo);
	}

	@Test
	void alterarPrecoDoPacote_naoMudaReservaExistente() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		String codigo = criarReserva(clienteId, saidaId, 2);

		mockMvc.perform(put("/api/pacotes/" + pacoteId).contentType(MediaType.APPLICATION_JSON).content("""
				{"destinoId": %d, "nome": "Atins 5 dias", "duracaoDias": 5, "precoBase": 2000.00, "ativo": true}
				""".formatted(destinoId)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/reservas/" + codigo))
				.andExpect(jsonPath("$.valorUnitario").value(1500.0))
				.andExpect(jsonPath("$.valorTotal").value(3000.0));
	}

	@Test
	void grupoDe6_aplicaDesconto() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);

		mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(clienteId, saidaId, 6)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.percentualDesconto").value(0.1))
				.andExpect(jsonPath("$.valorTotal").value(8100.0));
	}

	@Test
	void saidaSemVagas_retorna409ComVagasDisponiveis() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 3);
		criarReserva(clienteId, saidaId, 2);

		mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(clienteId, saidaId, 2)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "vagas-insuficientes"))
				.andExpect(jsonPath("$.vagasDisponiveis").value(1));
		assertVagas(saidaId, 1);
	}

	@Test
	void qtdZero_retorna400() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);

		mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(clienteId, saidaId, 0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("qtdPassageiros"));
	}

	@Test
	void pacoteInativo_retorna409() throws Exception {
		long inativo = criarPacote(destinoId, "Fora de linha", 5, false);
		long saidaId = criarSaida(inativo, "2026-07-10", 10);

		mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(clienteId, saidaId, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "operacao-invalida"));
		assertVagas(saidaId, 10);
	}

	/** Review Focus 5: saída ainda ABERTA, mas a data já passou. */
	@Test
	void saidaQueJaPartiu_retorna409SemDebitarVagas() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-06-10", 10);
		relogio.avancar(Duration.ofDays(30));

		mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(clienteId, saidaId, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "operacao-invalida"));
		assertVagas(saidaId, 10);
	}

	@Test
	void saidaEncerrada_retorna409() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		mockMvc.perform(patch("/api/saidas/" + saidaId + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "ENCERRADA"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(clienteId, saidaId, 1)))
				.andExpect(status().isConflict());
	}

	@Test
	void clienteInexistente_retorna404() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);

		mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
				.content(reservaJson(999, saidaId, 1)))
				.andExpect(status().isNotFound());
		assertVagas(saidaId, 10);
	}

	@Test
	void cancelar_devolveVagas() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		String codigo = criarReserva(clienteId, saidaId, 3);
		relogio.avancar(Duration.ofHours(2));

		mockMvc.perform(post("/api/reservas/" + codigo + "/cancelamento"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADA"))
				.andExpect(jsonPath("$.canceladaEm").value("2026-06-01T14:00:00Z"));
		assertVagas(saidaId, 10);
	}

	@Test
	void cancelarDuasVezes_retorna409() throws Exception {
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		String codigo = criarReserva(clienteId, saidaId, 3);
		mockMvc.perform(post("/api/reservas/" + codigo + "/cancelamento")).andExpect(status().isOk());

		mockMvc.perform(post("/api/reservas/" + codigo + "/cancelamento"))
				.andExpect(status().isConflict());
		assertVagas(saidaId, 10);
	}

	@Test
	void codigoInexistente_retorna404() throws Exception {
		mockMvc.perform(get("/api/reservas/RSV-XXXXXX"))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Content-Type", endsWith("problem+json")));
	}

	private void assertVagas(long saidaId, int esperadas) throws Exception {
		mockMvc.perform(get("/api/saidas/" + saidaId))
				.andExpect(jsonPath("$.vagasDisponiveis").value(esperadas));
	}

	private static String reservaJson(long clienteId, long saidaId, int qtd) {
		return """
				{"clienteId": %d, "saidaId": %d, "qtdPassageiros": %d}
				""".formatted(clienteId, saidaId, qtd);
	}

}
