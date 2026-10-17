package dev.carlosheitor.agencia.saida;

import java.time.LocalDate;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.pacote.Pacote;
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

/**
 * Uma data de partida de um pacote, com a sua lotação.
 * Toda mudança em vagas passa por reservarVagas/devolverVagas: a regra fica aqui, testável sem banco.
 * Proteger essas chamadas contra concorrência é papel do service (lock), não da entidade.
 */
@Entity
@Table(name = "saida")
public class Saida {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pacote_id")
	private Pacote pacote;

	@Column(name = "data_partida", nullable = false)
	private LocalDate dataPartida;

	@Column(name = "capacidade_total", nullable = false)
	private int capacidadeTotal;

	@Column(name = "vagas_disponiveis", nullable = false)
	private int vagasDisponiveis;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusSaida status;

	protected Saida() {
		// exigido pelo JPA
	}

	private Saida(Pacote pacote, LocalDate dataPartida, int capacidade) {
		this.pacote = pacote;
		this.dataPartida = dataPartida;
		this.capacidadeTotal = capacidade;
		this.vagasDisponiveis = capacidade;
		this.status = StatusSaida.ABERTA;
	}

	public static Saida nova(Pacote pacote, LocalDate dataPartida, int capacidade) {
		if (capacidade <= 0) {
			throw new IllegalArgumentException("Capacidade deve ser positiva: " + capacidade);
		}
		return new Saida(pacote, dataPartida, capacidade);
	}

	public void reservarVagas(int quantidade) {
		exigirQuantidadePositiva(quantidade);
		if (status != StatusSaida.ABERTA) {
			throw new OperacaoInvalidaException("Saída %d não está aberta para reservas (status: %s)".formatted(id, status));
		}
		if (quantidade > vagasDisponiveis) {
			throw new VagasInsuficientesException(vagasDisponiveis, quantidade);
		}
		vagasDisponiveis -= quantidade;
	}

	/** Devolver mais do que foi reservado é bug de quem chamou, não regra de negócio: por isso IllegalStateException. */
	public void devolverVagas(int quantidade) {
		exigirQuantidadePositiva(quantidade);
		if (vagasDisponiveis + quantidade > capacidadeTotal) {
			throw new IllegalStateException("Devolver %d vaga(s) passaria da capacidade (%d/%d)"
					.formatted(quantidade, vagasDisponiveis, capacidadeTotal));
		}
		vagasDisponiveis += quantidade;
	}

	/**
	 * ABERTA e ENCERRADA alternam livremente; CANCELADA é terminal e só vale sem reservas ativas.
	 * Toda reserva ativa ocupa vaga, então "sem reservas ativas" é o mesmo que "vagas = capacidade".
	 */
	public void alterarStatus(StatusSaida novo) {
		if (novo == status) {
			return;
		}
		if (status == StatusSaida.CANCELADA) {
			throw new OperacaoInvalidaException("Saída cancelada não muda mais de status");
		}
		if (novo == StatusSaida.CANCELADA && vagasDisponiveis < capacidadeTotal) {
			throw new OperacaoInvalidaException("Saída tem reservas ativas; cancele as reservas antes");
		}
		status = novo;
	}

	public LocalDate dataRetorno() {
		return dataPartida.plusDays(pacote.getDuracaoDias() - 1L);
	}

	/** A partir do dia da partida, não se reserva mais. */
	public boolean jaPartiu(LocalDate hoje) {
		return !dataPartida.isAfter(hoje);
	}

	private static void exigirQuantidadePositiva(int quantidade) {
		if (quantidade <= 0) {
			throw new IllegalArgumentException("Quantidade deve ser positiva: " + quantidade);
		}
	}

	public Long getId() {
		return id;
	}

	public Pacote getPacote() {
		return pacote;
	}

	public LocalDate getDataPartida() {
		return dataPartida;
	}

	public int getCapacidadeTotal() {
		return capacidadeTotal;
	}

	public int getVagasDisponiveis() {
		return vagasDisponiveis;
	}

	public StatusSaida getStatus() {
		return status;
	}

}
