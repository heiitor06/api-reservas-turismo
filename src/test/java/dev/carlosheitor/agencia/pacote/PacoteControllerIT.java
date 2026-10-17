package dev.carlosheitor.agencia.pacote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import dev.carlosheitor.agencia.support.IntegrationTestBase;

class PacoteControllerIT extends IntegrationTestBase {

	private static final String TIPO = "https://api.agencia.local/problemas/";

	@Test
	void criar_retorna201ComDestino() throws Exception {
		long destinoId = criarDestino("Lençóis Maranhenses");

		MvcResult resultado = mockMvc.perform(post("/api/pacotes").contentType(MediaType.APPLICATION_JSON)
				.content(pacoteJson(destinoId, "Lençóis 5 dias", 5, "1890.00", true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.destinoId").value(destinoId))
				.andExpect(jsonPath("$.destinoNome").value("Lençóis Maranhenses"))
				.andExpect(jsonPath("$.precoBase").value(1890.0))
				.andExpect(jsonPath("$.itens.length()").value(0))
				.andReturn();

		long pacoteId = idDe(resultado);
		assertThat(resultado.getResponse().getHeader("Location")).endsWith("/api/pacotes/" + pacoteId);
		mockMvc.perform(get("/api/pacotes/" + pacoteId)).andExpect(status().isOk());
	}

	@Test
	void criarComDestinoInexistente_retorna404() throws Exception {
		mockMvc.perform(post("/api/pacotes").contentType(MediaType.APPLICATION_JSON)
				.content(pacoteJson(999, "Fantasma", 3, "500.00", true)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Destino 999 não existe"));
	}

	@Test
	void precoBaseZero_retorna400() throws Exception {
		long destinoId = criarDestino("Atins");

		mockMvc.perform(post("/api/pacotes").contentType(MediaType.APPLICATION_JSON)
				.content(pacoteJson(destinoId, "Grátis", 3, "0", true)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("precoBase"));
	}

	@Test
	void listarFiltrandoInativos_retornaSoInativos() throws Exception {
		long destinoId = criarDestino("Atins");
		criarPacote(destinoId, "Ativo A", 3, true);
		criarPacote(destinoId, "Ativo B", 3, true);
		criarPacote(destinoId, "Inativo", 3, false);

		mockMvc.perform(get("/api/pacotes").param("ativo", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].nome").value("Inativo"))
				.andExpect(jsonPath("$.content[0].destinoNome").value("Atins"));
	}

	@Test
	void listarPorDestino_filtra() throws Exception {
		long atins = criarDestino("Atins");
		long jeri = criarDestino("Jericoacoara");
		criarPacote(atins, "Atins 3 dias", 3, true);
		criarPacote(jeri, "Jeri 4 dias", 4, true);
		criarPacote(jeri, "Jeri 7 dias", 7, true);

		mockMvc.perform(get("/api/pacotes").param("destinoId", String.valueOf(jeri)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].nome", contains("Jeri 4 dias", "Jeri 7 dias")));
	}

	@Test
	void adicionarItemComDiaMaiorQueDuracao_retorna409() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/itens").contentType(MediaType.APPLICATION_JSON)
				.content(itemJson("PASSEIO", "Passeio de quadriciclo", 6, 0)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "operacao-invalida"));
	}

	@Test
	void tipoDeItemInvalido_retorna400() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/itens").contentType(MediaType.APPLICATION_JSON)
				.content(itemJson("BARCO", "Travessia", 1, 0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	@Test
	void detalhar_retornaItensOrdenadosPorDiaEOrdem() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		adicionarItem(pacoteId, "PASSEIO", "Lagoa Azul", 2, 1);
		adicionarItem(pacoteId, "HOTEL", "Pousada", 1, 1);
		adicionarItem(pacoteId, "TRANSPORTE", "Transfer de São Luís", 1, 0);
		adicionarItem(pacoteId, "PASSEIO", "Rio Preguiças", 2, 0);

		mockMvc.perform(get("/api/pacotes/" + pacoteId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens[*].titulo",
						contains("Transfer de São Luís", "Pousada", "Rio Preguiças", "Lagoa Azul")));
	}

	@Test
	void adicionarItem_retorna201ComItemPersistido() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);

		mockMvc.perform(post("/api/pacotes/" + pacoteId + "/itens").contentType(MediaType.APPLICATION_JSON)
				.content(itemJson("HOTEL", "Pousada", 1, 0)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/pacotes/" + pacoteId)))
				.andExpect(jsonPath("$.itens[0].id").isNumber())
				.andExpect(jsonPath("$.itens[0].tipo").value("HOTEL"));
	}

	@Test
	void removerItem_retorna204ESomeDoDetalhe() throws Exception {
		long pacoteId = criarPacote(criarDestino("Atins"), "Atins 5 dias", 5, true);
		long itemId = adicionarItem(pacoteId, "HOTEL", "Pousada", 1, 0);
		adicionarItem(pacoteId, "PASSEIO", "Lagoa Azul", 2, 0);

		mockMvc.perform(delete("/api/pacotes/" + pacoteId + "/itens/" + itemId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/pacotes/" + pacoteId))
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].titulo").value("Lagoa Azul"));
	}

	@Test
	void removerItemDeOutroPacote_retorna404() throws Exception {
		long destinoId = criarDestino("Atins");
		long pacoteA = criarPacote(destinoId, "Pacote A", 5, true);
		long pacoteB = criarPacote(destinoId, "Pacote B", 5, true);
		long itemDeA = adicionarItem(pacoteA, "HOTEL", "Pousada", 1, 0);

		mockMvc.perform(delete("/api/pacotes/" + pacoteB + "/itens/" + itemDeA))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/pacotes/" + pacoteA))
				.andExpect(jsonPath("$.itens.length()").value(1));
	}

	@Test
	void reduzirDuracaoAbaixoDeItemExistente_retorna409() throws Exception {
		long destinoId = criarDestino("Atins");
		long pacoteId = criarPacote(destinoId, "Atins 5 dias", 5, true);
		adicionarItem(pacoteId, "PASSEIO", "Despedida", 5, 0);

		mockMvc.perform(put("/api/pacotes/" + pacoteId).contentType(MediaType.APPLICATION_JSON)
				.content(pacoteJson(destinoId, "Atins 3 dias", 3, "990.00", true)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "operacao-invalida"));

		mockMvc.perform(get("/api/pacotes/" + pacoteId))
				.andExpect(jsonPath("$.duracaoDias").value(5));
	}

	@Test
	void atualizar_trocaDestinoEPreco() throws Exception {
		long atins = criarDestino("Atins");
		long jeri = criarDestino("Jericoacoara");
		long pacoteId = criarPacote(atins, "Litoral", 4, true);

		mockMvc.perform(put("/api/pacotes/" + pacoteId).contentType(MediaType.APPLICATION_JSON)
				.content(pacoteJson(jeri, "Litoral", 4, "2100.50", false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.destinoNome").value("Jericoacoara"))
				.andExpect(jsonPath("$.precoBase").value(2100.5))
				.andExpect(jsonPath("$.ativo").value(false));
	}

	private long adicionarItem(long pacoteId, String tipo, String titulo, int dia, int ordem) throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/pacotes/" + pacoteId + "/itens").contentType(MediaType.APPLICATION_JSON)
				.content(itemJson(tipo, titulo, dia, ordem)))
				.andExpect(status().isCreated())
				.andReturn();
		List<Number> ids = JsonPath.read(resultado.getResponse().getContentAsString(),
				"$.itens[?(@.titulo == '" + titulo + "')].id");
		return ids.getFirst().longValue();
	}

	private static String pacoteJson(long destinoId, String nome, int duracaoDias, String precoBase, boolean ativo) {
		return """
				{"destinoId": %d, "nome": "%s", "descricao": "Roteiro pelos Lençóis", "duracaoDias": %d,
				 "precoBase": %s, "ativo": %b}
				""".formatted(destinoId, nome, duracaoDias, precoBase, ativo);
	}

	private static String itemJson(String tipo, String titulo, int dia, int ordem) {
		return """
				{"tipo": "%s", "titulo": "%s", "dia": %d, "ordem": %d}
				""".formatted(tipo, titulo, dia, ordem);
	}

}
