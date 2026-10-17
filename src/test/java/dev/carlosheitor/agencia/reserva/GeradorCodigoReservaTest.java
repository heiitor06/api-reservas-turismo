package dev.carlosheitor.agencia.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GeradorCodigoReservaTest {

	private final GeradorCodigoReserva gerador = new GeradorCodigoReserva();

	@Test
	void codigo_temPrefixoESeisCaracteresSemAmbiguos() {
		// sem 0/O e 1/I/L: o código é ditado por telefone e digitado pelo cliente
		assertThat(gerador.gerar()).matches("^RSV-[A-HJKMNP-Z2-9]{6}$");
	}

	@Test
	void codigos_naoSeRepetemEmPoucasGeracoes() {
		Set<String> codigos = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			codigos.add(gerador.gerar());
		}
		assertThat(codigos).hasSize(100);
	}

}
