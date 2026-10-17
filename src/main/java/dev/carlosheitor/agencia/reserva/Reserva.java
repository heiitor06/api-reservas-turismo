package dev.carlosheitor.agencia.reserva;

import java.math.BigDecimal;
import java.time.Instant;

import dev.carlosheitor.agencia.cliente.Cliente;
import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.preco.CalculoReserva;
import dev.carlosheitor.agencia.saida.Saida;
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
 * Criar e cancelar mexem nas vagas da saída aqui dentro, para não existir reserva ativa sem vaga debitada
 * nem cancelamento sem devolução. Transições válidas:
 * PENDENTE → CONFIRMADA, PENDENTE → CANCELADA, CONFIRMADA → CANCELADA.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 10, unique = true)
	private String codigo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "saida_id")
	private Saida saida;

	@Column(name = "qtd_passageiros", nullable = false)
	private int qtdPassageiros;

	@Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal valorUnitario;

	@Column(name = "percentual_desconto", nullable = false, precision = 5, scale = 4)
	private BigDecimal percentualDesconto;

	@Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal valorTotal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusReserva status;

	@Column(name = "criada_em", nullable = false)
	private Instant criadaEm;

	@Column(name = "cancelada_em")
	private Instant canceladaEm;

	protected Reserva() {
		// exigido pelo JPA
	}

	public static Reserva criar(String codigo, Cliente cliente, Saida saida, int qtdPassageiros,
			CalculoReserva calculo, Instant agora) {
		saida.reservarVagas(qtdPassageiros);

		Reserva reserva = new Reserva();
		reserva.codigo = codigo;
		reserva.cliente = cliente;
		reserva.saida = saida;
		reserva.qtdPassageiros = qtdPassageiros;
		reserva.valorUnitario = calculo.valorUnitario();
		reserva.percentualDesconto = calculo.percentualDesconto();
		reserva.valorTotal = calculo.valorTotal();
		reserva.status = StatusReserva.PENDENTE;
		reserva.criadaEm = agora;
		return reserva;
	}

	public void confirmar() {
		if (status != StatusReserva.PENDENTE) {
			throw new OperacaoInvalidaException("Só reserva pendente pode ser confirmada (status: %s)".formatted(status));
		}
		status = StatusReserva.CONFIRMADA;
	}

	public void cancelar(Instant agora) {
		if (status == StatusReserva.CANCELADA) {
			throw new OperacaoInvalidaException("Reserva %s já está cancelada".formatted(codigo));
		}
		saida.devolverVagas(qtdPassageiros);
		status = StatusReserva.CANCELADA;
		canceladaEm = agora;
	}

	public Long getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public Saida getSaida() {
		return saida;
	}

	public int getQtdPassageiros() {
		return qtdPassageiros;
	}

	public BigDecimal getValorUnitario() {
		return valorUnitario;
	}

	public BigDecimal getPercentualDesconto() {
		return percentualDesconto;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public StatusReserva getStatus() {
		return status;
	}

	public Instant getCriadaEm() {
		return criadaEm;
	}

	public Instant getCanceladaEm() {
		return canceladaEm;
	}

}
