package dev.carlosheitor.agencia.destino;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "destino")
public class Destino {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, length = 120)
	private String cidade;

	@Column(length = 2)
	private String uf;

	@Column(nullable = false, length = 60)
	private String pais;

	@Column(columnDefinition = "text")
	private String descricao;

	protected Destino() {
		// exigido pelo JPA
	}

	public Destino(String nome, String cidade, String uf, String pais, String descricao) {
		atualizar(nome, cidade, uf, pais, descricao);
	}

	public void atualizar(String nome, String cidade, String uf, String pais, String descricao) {
		this.nome = nome;
		this.cidade = cidade;
		this.uf = uf;
		this.pais = pais;
		this.descricao = descricao;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getCidade() {
		return cidade;
	}

	public String getUf() {
		return uf;
	}

	public String getPais() {
		return pais;
	}

	public String getDescricao() {
		return descricao;
	}

}
