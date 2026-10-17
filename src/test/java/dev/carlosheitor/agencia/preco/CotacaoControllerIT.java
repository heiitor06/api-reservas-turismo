package dev.carlosheitor.agencia.preco;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import dev.carlosheitor.agencia.support.IntegrationTestBase;

class CotacaoControllerIT extends IntegrationTestBase {

	private static final String TIPO = "https://api.agencia.local/problemas/";

	@Test
	void cotarGrupoNoCartao6x() throws Exception {
		long saidaId = saidaDePacoteDe1500();

		mockMvc.perform(post("/api/cotacoes").contentType(MediaType.APPLICATION_JSON)
				.content(cotacaoJson(saidaId, 6, "CARTAO", 6)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valorUnitario").value(1500.0))
				.andExpect(jsonPath("$.subtotal").value(9000.0))
				.andExpect(jsonPath("$.percentualDesconto").value(0.1))
				.andExpect(jsonPath("$.valorDesconto").value(900.0))
				.andExpect(jsonPath("$.valorTotal").value(8100.0))
				.andExpect(jsonPath("$.taxaParcelamento").value(0.045))
				.andExpect(jsonPath("$.valorCobrado").value(8464.5))
				.andExpect(jsonPath("$.parcelas").value(6))
				.andExpect(jsonPath("$.valorParcela").value(1410.75))
				.andExpect(jsonPath("$.vagasDisponiveis").value(20));
	}

	@Test
	void cotarNoPix_semTaxa() throws Exception {
		long saidaId = saidaDePacoteDe1500();

		mockMvc.perform(post("/api/cotacoes").contentType(MediaType.APPLICATION_JSON)
				.content(cotacaoJson(saidaId, 2, "PIX", 1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valorTotal").value(3000.0))
				.andExpect(jsonPath("$.valorCobrado").value(3000.0));
	}

	@Test
	void pixEm3x_retorna400() throws Exception {
		long saidaId = saidaDePacoteDe1500();

		mockMvc.perform(post("/api/cotacoes").contentType(MediaType.APPLICATION_JSON)
				.content(cotacaoJson(saidaId, 2, "PIX", 3)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	@Test
	void cartaoEm11x_retorna400() throws Exception {
		long saidaId = saidaDePacoteDe1500();

		mockMvc.perform(post("/api/cotacoes").contentType(MediaType.APPLICATION_JSON)
				.content(cotacaoJson(saidaId, 2, "CARTAO", 11)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	@Test
	void qtdZero_retorna400() throws Exception {
		long saidaId = saidaDePacoteDe1500();

		mockMvc.perform(post("/api/cotacoes").contentType(MediaType.APPLICATION_JSON)
				.content(cotacaoJson(saidaId, 0, "PIX", 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("qtdPassageiros"));
	}

	@Test
	void saidaInexistente_retorna404() throws Exception {
		mockMvc.perform(post("/api/cotacoes").contentType(MediaType.APPLICATION_JSON)
				.content(cotacaoJson(999, 2, "PIX", 1)))
				.andExpect(status().isNotFound());
	}

	private long saidaDePacoteDe1500() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		return criarSaida(pacoteId, "2026-07-10", 20);
	}

	private static String cotacaoJson(long saidaId, int qtd, String forma, int parcelas) {
		return """
				{"saidaId": %d, "qtdPassageiros": %d, "forma": "%s", "parcelas": %d}
				""".formatted(saidaId, qtd, forma, parcelas);
	}

}
