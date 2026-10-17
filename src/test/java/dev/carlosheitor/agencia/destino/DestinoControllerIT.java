package dev.carlosheitor.agencia.destino;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import dev.carlosheitor.agencia.support.IntegrationTestBase;

class DestinoControllerIT extends IntegrationTestBase {

	private static final String TIPO = "https://api.agencia.local/problemas/";

	@Test
	void criar_retorna201ComLocation() throws Exception {
		mockMvc.perform(post("/api/destinos").contentType(MediaType.APPLICATION_JSON).content(destinoJson("Lençóis Maranhenses")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/destinos/1")))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nome").value("Lençóis Maranhenses"))
				.andExpect(jsonPath("$.uf").value("MA"));
	}

	@Test
	void buscarInexistente_retorna404ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/destinos/999"))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Content-Type", "application/problem+json"))
				.andExpect(jsonPath("$.type").value(TIPO + "recurso-nao-encontrado"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("Destino 999 não existe"));
	}

	@Test
	void criarComNomeEmBranco_retorna400ComErros() throws Exception {
		mockMvc.perform(post("/api/destinos").contentType(MediaType.APPLICATION_JSON).content(destinoJson(" ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"))
				.andExpect(jsonPath("$.erros.length()").value(1))
				.andExpect(jsonPath("$.erros[0].campo").value("nome"))
				.andExpect(jsonPath("$.erros[0].mensagem").value("não deve estar em branco"));
	}

	@Test
	void jsonMalformado_retorna400() throws Exception {
		mockMvc.perform(post("/api/destinos").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	@Test
	void listar_retornaPaginado() throws Exception {
		criar("Barreirinhas");
		criar("Atins");
		criar("Santo Amaro");

		mockMvc.perform(get("/api/destinos").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].nome").value("Atins"))
				.andExpect(jsonPath("$.page.totalElements").value(3))
				.andExpect(jsonPath("$.page.totalPages").value(2));
	}

	@Test
	void listarComOrdenacaoInvalida_retorna400() throws Exception {
		mockMvc.perform(get("/api/destinos").param("sort", "campoQueNaoExiste"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	@Test
	void idNaoNumerico_retorna400() throws Exception {
		mockMvc.perform(get("/api/destinos/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(TIPO + "dados-invalidos"));
	}

	@Test
	void atualizar_alteraCampos() throws Exception {
		criar("Barreirinhas");

		mockMvc.perform(put("/api/destinos/1").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome": "Jericoacoara", "cidade": "Jijoca de Jericoacoara", "uf": "CE", "pais": "Brasil"}
				"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Jericoacoara"));

		mockMvc.perform(get("/api/destinos/1"))
				.andExpect(jsonPath("$.nome").value("Jericoacoara"))
				.andExpect(jsonPath("$.uf").value("CE"))
				.andExpect(jsonPath("$.descricao").doesNotExist());
	}

	private void criar(String nome) throws Exception {
		mockMvc.perform(post("/api/destinos").contentType(MediaType.APPLICATION_JSON).content(destinoJson(nome)))
				.andExpect(status().isCreated());
	}

	private static String destinoJson(String nome) {
		return """
				{"nome": "%s", "cidade": "Barreirinhas", "uf": "MA", "pais": "Brasil", "descricao": "Dunas e lagoas"}
				""".formatted(nome);
	}

}
