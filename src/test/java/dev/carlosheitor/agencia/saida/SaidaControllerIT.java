package dev.carlosheitor.agencia.saida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import dev.carlosheitor.agencia.support.IntegrationTestBase;

class SaidaControllerIT extends IntegrationTestBase {

	private static final String TIPO = "https://api.agencia.local/problemas/";

	// O relógio dos testes começa em 2026-06-01

	@Test
	void criar_iniciaComVagasIguaisACapacidade() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas").contentType(MediaType.APPLICATION_JSON)
				.content(saidaJson("2026-07-10", 20)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pacoteId").value(pacoteId))
				.andExpect(jsonPath("$.pacoteNome").value("Atins 5 dias"))
				.andExpect(jsonPath("$.dataPartida").value("2026-07-10"))
				.andExpect(jsonPath("$.dataRetorno").value("2026-07-14"))
				.andExpect(jsonPath("$.capacidadeTotal").value(20))
				.andExpect(jsonPath("$.vagasDisponiveis").value(20))
				.andExpect(jsonPath("$.status").value("ABERTA"));
	}

	@Test
	void criarComDataPassada_retorna409() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas").contentType(MediaType.APPLICATION_JSON)
				.content(saidaJson("2026-05-20", 20)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "operacao-invalida"));
	}

	@Test
	void criarParaHoje_retorna409() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas").contentType(MediaType.APPLICATION_JSON)
				.content(saidaJson("2026-06-01", 20)))
				.andExpect(status().isConflict());
	}

	/** 23h30 em Brasília de 01/06 já é 02/06 em UTC; o "hoje" do negócio é o do Brasil. */
	@Test
	void hojeEhCalculadoNoFusoDaAgencia() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		relogio.definir(Instant.parse("2026-06-02T02:30:00Z"));

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas").contentType(MediaType.APPLICATION_JSON)
				.content(saidaJson("2026-06-02", 20)))
				.andExpect(status().isCreated());
	}

	@Test
	void criarDuplicada_retorna409RecursoDuplicado() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		criarSaida(pacoteId, "2026-07-10", 20);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas").contentType(MediaType.APPLICATION_JSON)
				.content(saidaJson("2026-07-10", 30)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "recurso-duplicado"));
	}

	/**
	 * Todas passam juntas pela checagem "já existe?" e só a UNIQUE do banco segura.
	 * O teste garante que a violação da constraint vira 409, e não 500.
	 */
	@Test
	void criarMesmaSaidaEmParalelo_umaCriaAsOutrasRecebem409() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		int requisicoes = 6;
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<Integer>> respostas = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(requisicoes)) {
			for (int i = 0; i < requisicoes; i++) {
				respostas.add(executor.submit(() -> {
					largada.await();
					return mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas")
							.contentType(MediaType.APPLICATION_JSON).content(saidaJson("2026-07-10", 20)))
							.andReturn().getResponse().getStatus();
				}));
			}
			largada.countDown();
		}

		List<Integer> status = new ArrayList<>();
		for (Future<Integer> resposta : respostas) {
			status.add(resposta.get());
		}
		assertThat(status).containsOnly(201, 409);
		assertThat(status).filteredOn(s -> s == 201).hasSize(1);
	}

	@Test
	void criarEmPacoteInexistente_retorna404() throws Exception {
		mockMvc.perform(post("/api/pacotes/999/saidas").contentType(MediaType.APPLICATION_JSON)
				.content(saidaJson("2026-07-10", 20)))
				.andExpect(status().isNotFound());
	}

	@Test
	void capacidadeZero_retorna400() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/saidas").contentType(MediaType.APPLICATION_JSON)
				.content(saidaJson("2026-07-10", 0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("capacidadeTotal"));
	}

	@Test
	void listarPorPacote_ordenaPorData() throws Exception {
		long destinoId = criarDestino("Atins");
		long pacoteId = criarPacote(destinoId, "Atins 5 dias", 5, true);
		long outroPacote = criarPacote(destinoId, "Outro", 3, true);
		criarSaida(pacoteId, "2026-09-01", 20);
		criarSaida(pacoteId, "2026-07-10", 20);
		criarSaida(outroPacote, "2026-08-01", 20);

		mockMvc.perform(get("/api/pacotes/" + pacoteId + "/saidas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].dataPartida", contains("2026-07-10", "2026-09-01")));
	}

	@Test
	void buscar_retornaSaida() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		long saidaId = criarSaida(pacoteId, "2026-07-10", 20);

		mockMvc.perform(get("/api/saidas/" + saidaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(saidaId))
				.andExpect(jsonPath("$.dataRetorno").value("2026-07-14"));
	}

	@Test
	void buscarInexistente_retorna404() throws Exception {
		mockMvc.perform(get("/api/saidas/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Saída 999 não existe"));
	}

	@Test
	void alterarStatusParaEncerrada_retorna200() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		long saidaId = criarSaida(pacoteId, "2026-07-10", 20);

		mockMvc.perform(patch("/api/saidas/" + saidaId + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "ENCERRADA"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENCERRADA"));
	}

	@Test
	void canceladaNaoReabre_retorna409() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		long saidaId = criarSaida(pacoteId, "2026-07-10", 20);
		mockMvc.perform(patch("/api/saidas/" + saidaId + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "CANCELADA"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/saidas/" + saidaId + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "ABERTA"}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void statusInvalidoNoCorpo_retorna400() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		long saidaId = criarSaida(pacoteId, "2026-07-10", 20);

		mockMvc.perform(patch("/api/saidas/" + saidaId + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "LOTADA"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	private static String saidaJson(String dataPartida, int capacidade) {
		return """
				{"dataPartida": "%s", "capacidadeTotal": %d}
				""".formatted(dataPartida, capacidade);
	}

}
