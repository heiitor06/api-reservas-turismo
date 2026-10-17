package dev.carlosheitor.agencia.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.carlosheitor.agencia.support.IntegrationTestBase;

class ClienteControllerIT extends IntegrationTestBase {

	private static final String TIPO = "https://api.agencia.local/problemas/";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void criar_retorna201ComDocumentoMascarado() throws Exception {
		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(clienteJson("Maria Souza", "529.982.247-25", "maria@email.com")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/clientes/1")))
				.andExpect(jsonPath("$.nome").value("Maria Souza"))
				.andExpect(jsonPath("$.documento").value("***.982.247-**"))
				.andExpect(jsonPath("$.email").value("maria@email.com"));
	}

	@Test
	void documento_eGravadoSoComDigitos() throws Exception {
		criar("Maria Souza", "529.982.247-25", "maria@email.com");

		String gravado = jdbc.queryForObject("select documento from cliente", String.class);
		assertThat(gravado).isEqualTo("52998224725");
	}

	/** Review Focus 3: com e sem máscara são o mesmo CPF. */
	@Test
	void mesmoCpfComESemMascara_segundoRetorna409() throws Exception {
		criar("Maria Souza", "529.982.247-25", "maria@email.com");

		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(clienteJson("Maria S.", "52998224725", "outra@email.com")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(TIPO + "recurso-duplicado"));
	}

	@Test
	void cpfComDigitoInvalido_retorna400() throws Exception {
		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(clienteJson("Maria Souza", "529.982.247-26", "maria@email.com")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("documento"));
	}

	@Test
	void cpfComTodosDigitosIguais_retorna400() throws Exception {
		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(clienteJson("Maria Souza", "111.111.111-11", "maria@email.com")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("documento"));
	}

	@Test
	void emailInvalido_retorna400() throws Exception {
		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(clienteJson("Maria Souza", "529.982.247-25", "maria.email.com")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("email"));
	}

	@Test
	void buscar_retornaCliente() throws Exception {
		criar("Maria Souza", "529.982.247-25", "maria@email.com");

		mockMvc.perform(get("/api/clientes/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.telefone").value("(98) 99999-0000"))
				.andExpect(jsonPath("$.documento").value("***.982.247-**"));
	}

	@Test
	void buscarInexistente_retorna404() throws Exception {
		mockMvc.perform(get("/api/clientes/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Cliente 999 não existe"));
	}

	@Test
	void listar_paginadoPorNome() throws Exception {
		criar("Pedro", "111.444.777-35", "pedro@email.com");
		criar("Ana", "168.995.350-09", "ana@email.com");
		criar("Maria", "390.533.447-05", "maria@email.com");

		mockMvc.perform(get("/api/clientes").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].nome", contains("Ana", "Maria")))
				.andExpect(jsonPath("$.page.totalElements").value(3));
	}

	private void criar(String nome, String documento, String email) throws Exception {
		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(clienteJson(nome, documento, email)))
				.andExpect(status().isCreated());
	}

	private static String clienteJson(String nome, String documento, String email) {
		return """
				{"nome": "%s", "documento": "%s", "email": "%s", "telefone": "(98) 99999-0000"}
				""".formatted(nome, documento, email);
	}

}
