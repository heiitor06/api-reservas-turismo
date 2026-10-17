package dev.carlosheitor.agencia.cliente;

/** Formatação de CPF. A validação dos dígitos fica com o @CPF do Hibernate Validator no DTO. */
final class Cpf {

	private Cpf() {
	}

	static String normalizar(String cpf) {
		return cpf.replaceAll("\\D", "");
	}

	/** Mostra só os 6 dígitos do meio: identifica o cliente sem expor o documento inteiro. */
	static String mascarar(String cpfNormalizado) {
		return "***.%s.%s-**".formatted(cpfNormalizado.substring(3, 6), cpfNormalizado.substring(6, 9));
	}

}
