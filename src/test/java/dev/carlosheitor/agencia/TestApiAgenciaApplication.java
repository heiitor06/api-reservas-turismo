package dev.carlosheitor.agencia;

import org.springframework.boot.SpringApplication;

public class TestApiAgenciaApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiAgenciaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
