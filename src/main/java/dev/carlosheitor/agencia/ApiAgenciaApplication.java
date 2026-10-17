package dev.carlosheitor.agencia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiAgenciaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiAgenciaApplication.class, args);
	}

}
