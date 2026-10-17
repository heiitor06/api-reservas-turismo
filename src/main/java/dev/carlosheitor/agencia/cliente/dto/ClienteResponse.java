package dev.carlosheitor.agencia.cliente.dto;

import dev.carlosheitor.agencia.cliente.Cliente;

/** O documento sai mascarado: dado pessoal (LGPD) não volta inteiro em resposta nem em listagem. */
public record ClienteResponse(Long id, String nome, String documento, String email, String telefone) {

	public static ClienteResponse from(Cliente cliente) {
		return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getDocumentoMascarado(),
				cliente.getEmail(), cliente.getTelefone());
	}

}
