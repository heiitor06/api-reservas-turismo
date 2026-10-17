package dev.carlosheitor.agencia.compartilhado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	@Bean
	OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("API de Reservas — Agência de Turismo")
				.version("v1")
				.description("""
						Destinos, pacotes, saídas com controle de vagas, reservas e pagamentos.
						Reservas simultâneas na mesma saída são serializadas por lock pessimista: sem overbooking.
						Erros seguem a RFC 9457 (application/problem+json); o campo `type` identifica o problema.
						Pagamentos são simulados: aprovação e recusa fazem o papel do webhook de um gateway."""));
	}

}
