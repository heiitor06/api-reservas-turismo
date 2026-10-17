package dev.carlosheitor.agencia.pacote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Parte do agregado Pacote: só é criado e removido através dele. */
@Entity
@Table(name = "item_roteiro")
public class ItemRoteiro {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pacote_id")
	private Pacote pacote;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoItem tipo;

	@Column(nullable = false, length = 150)
	private String titulo;

	@Column(columnDefinition = "text")
	private String descricao;

	@Column(nullable = false)
	private int dia;

	@Column(nullable = false)
	private int ordem;

	protected ItemRoteiro() {
		// exigido pelo JPA
	}

	ItemRoteiro(Pacote pacote, TipoItem tipo, String titulo, String descricao, int dia, int ordem) {
		this.pacote = pacote;
		this.tipo = tipo;
		this.titulo = titulo;
		this.descricao = descricao;
		this.dia = dia;
		this.ordem = ordem;
	}

	public Long getId() {
		return id;
	}

	public TipoItem getTipo() {
		return tipo;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public int getDia() {
		return dia;
	}

	public int getOrdem() {
		return ordem;
	}

}
