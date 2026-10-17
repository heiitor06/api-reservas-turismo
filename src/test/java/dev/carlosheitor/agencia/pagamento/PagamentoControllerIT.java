package dev.carlosheitor.agencia.pagamento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import dev.carlosheitor.agencia.support.IntegrationTestBase;

/** Reserva base: 2 passageiros × 1500,00 = 3000,00. */
class PagamentoControllerIT extends IntegrationTestBase {

	private static final String TIPO = "https://api.agencia.local/problemas/";

	private String codigo;

	@BeforeEach
	void cenario() throws Exception {
		long clienteId = criarCliente("Maria Souza", "529.982.247-25");
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		long saidaId = criarSaida(pacoteId, "2026-07-10", 10);
		codigo = criarReserva(clienteId, saidaId, 2);
	}

	@Test
	void pagamentoIntegralAprovado_confirmaReserva() throws Exception {
		long pagamentoId = registrar("3000.00", "PIX", 1);

		mockMvc.perform(post("/api/pagamentos/" + pagamentoId + "/aprovacao"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APROVADO"))
				.andExpect(jsonPath("$.reservaStatus").value("CONFIRMADA"));

		mockMvc.perform(get("/api/reservas/" + codigo))
				.andExpect(jsonPath("$.status").value("CONFIRMADA"))
				.andExpect(jsonPath("$.valorPago").value(3000.0))
				.andExpect(jsonPath("$.saldo").value(0));
	}

	@Test
	void registrar_retorna201Pendente() throws Exception {
		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("900.00", "PIX", 1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.reservaCodigo").value(codigo))
				.andExpect(jsonPath("$.reservaStatus").value("PENDENTE"))
				.andExpect(jsonPath("$.valor").value(900.0))
				.andExpect(jsonPath("$.criadoEm").value("2026-06-01T12:00:00Z"));
	}

	@Test
	void sinalMaisSaldo_confirmaSoNoSegundo() throws Exception {
		aprovar(registrar("900.00", "PIX", 1));

		mockMvc.perform(get("/api/reservas/" + codigo))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.valorPago").value(900.0))
				.andExpect(jsonPath("$.saldo").value(2100.0));

		aprovar(registrar("2100.00", "CARTAO", 3));

		mockMvc.perform(get("/api/reservas/" + codigo))
				.andExpect(jsonPath("$.status").value("CONFIRMADA"))
				.andExpect(jsonPath("$.valorPago").value(3000.0))
				.andExpect(jsonPath("$.pagamentos.length()").value(2));
	}

	@Test
	void valorAcimaDoSaldo_retorna409ComSaldo() throws Exception {
		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("3000.01", "PIX", 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "saldo-excedido"))
				.andExpect(jsonPath("$.saldo").value(3000.0));
	}

	@Test
	void pagamentoPendenteSeguraSaldo() throws Exception {
		registrar("3000.00", "PIX", 1);

		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("1.00", "PIX", 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.saldo").value(0));
	}

	@Test
	void recusadoLiberaSaldo() throws Exception {
		long recusado = registrar("3000.00", "BOLETO", 1);
		mockMvc.perform(post("/api/pagamentos/" + recusado + "/recusa"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RECUSADO"));

		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("3000.00", "PIX", 1)))
				.andExpect(status().isCreated());
	}

	@Test
	void cartao6x_cobraComTaxa() throws Exception {
		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("3000.00", "CARTAO", 6)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.taxaPercentual").value(0.045))
				.andExpect(jsonPath("$.valorCobrado").value(3135.0));
	}

	@Test
	void pixEm3x_retorna400() throws Exception {
		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("3000.00", "PIX", 3)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	@Test
	void valorComTresCasas_retorna400() throws Exception {
		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("100.001", "PIX", 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("valor"));
	}

	@Test
	void aprovarDeReservaCancelada_retorna409EPagamentoContinuaPendente() throws Exception {
		long pagamentoId = registrar("3000.00", "PIX", 1);
		mockMvc.perform(post("/api/reservas/" + codigo + "/cancelamento")).andExpect(status().isOk());

		mockMvc.perform(post("/api/pagamentos/" + pagamentoId + "/aprovacao"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "operacao-invalida"));

		mockMvc.perform(get("/api/pagamentos/" + pagamentoId))
				.andExpect(jsonPath("$.status").value("PENDENTE"));
	}

	@Test
	void aprovarJaAprovado_retorna409() throws Exception {
		long pagamentoId = registrar("900.00", "PIX", 1);
		aprovar(pagamentoId);

		mockMvc.perform(post("/api/pagamentos/" + pagamentoId + "/aprovacao"))
				.andExpect(status().isConflict());
	}

	@Test
	void pagarReservaConfirmada_retorna409() throws Exception {
		aprovar(registrar("3000.00", "PIX", 1));

		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("1.00", "PIX", 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "operacao-invalida"));
	}

	@Test
	void pagarReservaCancelada_retorna409() throws Exception {
		mockMvc.perform(post("/api/reservas/" + codigo + "/cancelamento")).andExpect(status().isOk());

		mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("100.00", "PIX", 1)))
				.andExpect(status().isConflict());
	}

	@Test
	void reservaInexistente_retorna404() throws Exception {
		mockMvc.perform(post("/api/reservas/RSV-XXXXXX/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson("100.00", "PIX", 1)))
				.andExpect(status().isNotFound());
	}

	@Test
	void aprovarPagamentoInexistente_retorna404() throws Exception {
		mockMvc.perform(post("/api/pagamentos/999/aprovacao"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Pagamento 999 não existe"));
	}

	private long registrar(String valor, String forma, int parcelas) throws Exception {
		return idDe(mockMvc.perform(post("/api/reservas/" + codigo + "/pagamentos").contentType(MediaType.APPLICATION_JSON)
				.content(pagamentoJson(valor, forma, parcelas)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	private void aprovar(long pagamentoId) throws Exception {
		mockMvc.perform(post("/api/pagamentos/" + pagamentoId + "/aprovacao")).andExpect(status().isOk());
	}

	private static String pagamentoJson(String valor, String forma, int parcelas) {
		return """
				{"valor": %s, "forma": "%s", "parcelas": %d}
				""".formatted(valor, forma, parcelas);
	}

}
