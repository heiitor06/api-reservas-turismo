package dev.carlosheitor.agencia.pagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.reserva.Reserva;
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

/** Aprovação e recusa são simuladas pela API (papel do webhook de um gateway). Só PENDENTE muda de status. */
@Entity
@Table(name = "pagamento")
public class Pagamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reserva_id")
	private Reserva reserva;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal valor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FormaPagamento forma;

	@Column(nullable = false)
	private int parcelas;

	@Column(name = "taxa_percentual", nullable = false, precision = 5, scale = 4)
	private BigDecimal taxaPercentual;

	@Column(name = "valor_cobrado", nullable = false, precision = 12, scale = 2)
	private BigDecimal valorCobrado;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusPagamento status;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Pagamento() {
		// exigido pelo JPA
	}

	public static Pagamento novo(Reserva reserva, BigDecimal valor, FormaPagamento forma, int parcelas,
			BigDecimal taxaPercentual, BigDecimal valorCobrado, Instant agora) {
		Pagamento pagamento = new Pagamento();
		pagamento.reserva = reserva;
		pagamento.valor = valor;
		pagamento.forma = forma;
		pagamento.parcelas = parcelas;
		pagamento.taxaPercentual = taxaPercentual;
		pagamento.valorCobrado = valorCobrado;
		pagamento.status = StatusPagamento.PENDENTE;
		pagamento.criadoEm = agora;
		return pagamento;
	}

	/** Única conta de saldo da aplicação: a regra de "pode pagar?" e a resposta da API usam a mesma. */
	public static BigDecimal somar(Collection<Pagamento> pagamentos, Set<StatusPagamento> status) {
		return pagamentos.stream()
				.filter(p -> status.contains(p.status))
				.map(p -> p.valor)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public void aprovar() {
		exigirPendente();
		status = StatusPagamento.APROVADO;
	}

	public void recusar() {
		exigirPendente();
		status = StatusPagamento.RECUSADO;
	}

	private void exigirPendente() {
		if (status != StatusPagamento.PENDENTE) {
			throw new OperacaoInvalidaException("Pagamento %d já está %s".formatted(id, status));
		}
	}

	public Long getId() {
		return id;
	}

	public Reserva getReserva() {
		return reserva;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public FormaPagamento getForma() {
		return forma;
	}

	public int getParcelas() {
		return parcelas;
	}

	public BigDecimal getTaxaPercentual() {
		return taxaPercentual;
	}

	public BigDecimal getValorCobrado() {
		return valorCobrado;
	}

	public StatusPagamento getStatus() {
		return status;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
