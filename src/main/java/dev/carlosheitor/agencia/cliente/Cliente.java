package dev.carlosheitor.agencia.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(nullable = false, length = 11, unique = true)
	private String documento;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(nullable = false, length = 20)
	private String telefone;

	protected Cliente() {
		// exigido pelo JPA
	}

	public Cliente(String nome, String documento, String email, String telefone) {
		this.nome = nome;
		this.documento = Cpf.normalizar(documento);
		this.email = email;
		this.telefone = telefone;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getDocumento() {
		return documento;
	}

	public String getDocumentoMascarado() {
		return Cpf.mascarar(documento);
	}

	public String getEmail() {
		return email;
	}

	public String getTelefone() {
		return telefone;
	}

}
