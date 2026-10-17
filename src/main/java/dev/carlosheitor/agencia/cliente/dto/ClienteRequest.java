package dev.carlosheitor.agencia.cliente.dto;

import org.hibernate.validator.constraints.br.CPF;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CPF aceito com ou sem máscara; os dígitos verificadores são conferidos pelo @CPF. */
public record ClienteRequest(
		@NotBlank @Size(max = 150) String nome,
		@NotBlank @CPF String documento,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(max = 20) String telefone) {
}
