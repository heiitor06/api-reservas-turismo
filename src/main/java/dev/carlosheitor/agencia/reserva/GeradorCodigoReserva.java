package dev.carlosheitor.agencia.reserva;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Código público da reserva (ex.: RSV-7K2QXM). Aleatório para não revelar quantas reservas existem,
 * como um id sequencial revelaria. Sem 0/O e 1/I/L: o código é ditado por telefone e digitado pelo cliente.
 * 31^6 ≈ 887 milhões de combinações; numa colisão (rara), a UNIQUE do banco recusa e a API devolve 409.
 */
@Component
public class GeradorCodigoReserva {

	private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	private static final int TAMANHO = 6;

	private final SecureRandom random = new SecureRandom();

	public String gerar() {
		StringBuilder codigo = new StringBuilder("RSV-");
		for (int i = 0; i < TAMANHO; i++) {
			codigo.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
		}
		return codigo.toString();
	}

}
