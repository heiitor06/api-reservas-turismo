package dev.carlosheitor.agencia.pacote;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.compartilhado.erro.RecursoNaoEncontradoException;
import dev.carlosheitor.agencia.destino.Destino;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Raiz do agregado: os itens do roteiro só mudam por aqui, e a regra
 * "item dentro da duração do pacote" vale tanto ao adicionar item quanto ao mudar a duração.
 */
@Entity
@Table(name = "pacote")
public class Pacote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destino_id")
	private Destino destino;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(columnDefinition = "text")
	private String descricao;

	@Column(name = "duracao_dias", nullable = false)
	private int duracaoDias;

	@Column(name = "preco_base", nullable = false, precision = 12, scale = 2)
	private BigDecimal precoBase;

	@Column(nullable = false)
	private boolean ativo;

	@OneToMany(mappedBy = "pacote", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ItemRoteiro> itens = new ArrayList<>();

	protected Pacote() {
		// exigido pelo JPA
	}

	public Pacote(Destino destino, String nome, String descricao, int duracaoDias, BigDecimal precoBase, boolean ativo) {
		atualizar(destino, nome, descricao, duracaoDias, precoBase, ativo);
	}

	public void atualizar(Destino destino, String nome, String descricao, int duracaoDias, BigDecimal precoBase,
			boolean ativo) {
		int ultimoDiaComItem = itens.stream().mapToInt(ItemRoteiro::getDia).max().orElse(0);
		if (duracaoDias < ultimoDiaComItem) {
			throw new OperacaoInvalidaException("O roteiro tem item no dia %d; a duração não pode ser menor que isso"
					.formatted(ultimoDiaComItem));
		}
		this.destino = destino;
		this.nome = nome;
		this.descricao = descricao;
		this.duracaoDias = duracaoDias;
		this.precoBase = precoBase;
		this.ativo = ativo;
	}

	public ItemRoteiro adicionarItem(TipoItem tipo, String titulo, String descricao, int dia, int ordem) {
		if (dia > duracaoDias) {
			throw new OperacaoInvalidaException("Dia %d fora da duração do pacote (%d dias)".formatted(dia, duracaoDias));
		}
		ItemRoteiro item = new ItemRoteiro(this, tipo, titulo, descricao, dia, ordem);
		itens.add(item);
		return item;
	}

	public void removerItem(Long itemId) {
		ItemRoteiro item = itens.stream()
				.filter(i -> i.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new RecursoNaoEncontradoException("Item de roteiro", itemId));
		itens.remove(item);
	}

	public Long getId() {
		return id;
	}

	public Destino getDestino() {
		return destino;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public int getDuracaoDias() {
		return duracaoDias;
	}

	public BigDecimal getPrecoBase() {
		return precoBase;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public List<ItemRoteiro> getItens() {
		return Collections.unmodifiableList(itens);
	}

}
