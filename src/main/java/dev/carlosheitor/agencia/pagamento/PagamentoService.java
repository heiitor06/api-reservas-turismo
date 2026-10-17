package dev.carlosheitor.agencia.pagamento;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.compartilhado.erro.RecursoNaoEncontradoException;
import dev.carlosheitor.agencia.pagamento.dto.PagamentoResponse;
import dev.carlosheitor.agencia.pagamento.dto.RegistrarPagamentoRequest;
import dev.carlosheitor.agencia.preco.CalculadoraPreco;
import dev.carlosheitor.agencia.reserva.Reserva;
import dev.carlosheitor.agencia.reserva.ReservaRepository;
import dev.carlosheitor.agencia.reserva.StatusReserva;

@Service
@Transactional(readOnly = true)
public class PagamentoService {

	private final PagamentoRepository repository;
	private final ReservaRepository reservaRepository;
	private final CalculadoraPreco calculadora;
	private final Clock clock;

	public PagamentoService(PagamentoRepository repository, ReservaRepository reservaRepository,
			CalculadoraPreco calculadora, Clock clock) {
		this.repository = repository;
		this.reservaRepository = reservaRepository;
		this.calculadora = calculadora;
		this.clock = clock;
	}

	/*
	 * Toda operação de pagamento trava a RESERVA primeiro. Assim, pagamentos da mesma reserva são processados
	 * um de cada vez, e cada um enxerga o que o anterior gravou (saldo, aprovações, status da reserva).
	 * Só a reserva é travada aqui; o cancelamento trava saída e depois reserva, então não há ciclo de espera.
	 */

	@Transactional
	public PagamentoResponse registrar(String codigoReserva, RegistrarPagamentoRequest request) {
		BigDecimal taxa = calculadora.taxaParcelamento(request.forma(), request.parcelas());
		Reserva reserva = reservaRepository.findByCodigoForUpdate(codigoReserva)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Reserva", codigoReserva));
		if (reserva.getStatus() != StatusReserva.PENDENTE) {
			throw new OperacaoInvalidaException("Reserva %s não aceita pagamento (status: %s)"
					.formatted(codigoReserva, reserva.getStatus()));
		}
		BigDecimal saldo = saldo(reserva);
		if (request.valor().compareTo(saldo) > 0) {
			throw new SaldoExcedidoException(request.valor(), saldo);
		}
		Pagamento pagamento = Pagamento.novo(reserva, request.valor(), request.forma(), request.parcelas(), taxa,
				calculadora.valorCobrado(request.valor(), taxa), clock.instant());
		return PagamentoResponse.from(repository.save(pagamento));
	}

	@Transactional
	public PagamentoResponse aprovar(Long pagamentoId) {
		Reserva reserva = travarReservaDoPagamento(pagamentoId);
		Pagamento pagamento = obterEntidade(pagamentoId);
		if (reserva.getStatus() == StatusReserva.CANCELADA) {
			throw new OperacaoInvalidaException("Reserva %s foi cancelada; o pagamento não pode ser aprovado"
					.formatted(reserva.getCodigo()));
		}
		pagamento.aprovar();
		BigDecimal pago = Pagamento.somar(pagamentosDa(reserva), StatusPagamento.APROVADOS);
		if (pago.compareTo(reserva.getValorTotal()) >= 0) {
			reserva.confirmar();
		}
		return PagamentoResponse.from(pagamento);
	}

	@Transactional
	public PagamentoResponse recusar(Long pagamentoId) {
		travarReservaDoPagamento(pagamentoId);
		Pagamento pagamento = obterEntidade(pagamentoId);
		pagamento.recusar();
		return PagamentoResponse.from(pagamento);
	}

	/**
	 * Descobre a reserva só pelo id (sem carregar o pagamento) e trava. O pagamento é lido depois do lock:
	 * se fosse carregado antes, o Hibernate devolveria a cópia em memória, com o status de antes da espera.
	 */
	private Reserva travarReservaDoPagamento(Long pagamentoId) {
		Long reservaId = repository.findReservaIdByPagamentoId(pagamentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", pagamentoId));
		return reservaRepository.findByIdForUpdate(reservaId).orElseThrow();
	}

	public PagamentoResponse buscar(Long pagamentoId) {
		return PagamentoResponse.from(obterEntidade(pagamentoId));
	}

	private BigDecimal saldo(Reserva reserva) {
		return reserva.getValorTotal().subtract(Pagamento.somar(pagamentosDa(reserva), StatusPagamento.COMPROMETIDOS));
	}

	private List<Pagamento> pagamentosDa(Reserva reserva) {
		return repository.findByReservaIdOrderByCriadoEmAscIdAsc(reserva.getId());
	}

	private Pagamento obterEntidade(Long pagamentoId) {
		return repository.findById(pagamentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", pagamentoId));
	}

}
