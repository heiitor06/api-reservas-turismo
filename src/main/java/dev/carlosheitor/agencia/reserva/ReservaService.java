package dev.carlosheitor.agencia.reserva;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.carlosheitor.agencia.cliente.Cliente;
import dev.carlosheitor.agencia.cliente.ClienteService;
import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.compartilhado.erro.RecursoNaoEncontradoException;
import dev.carlosheitor.agencia.preco.CalculadoraPreco;
import dev.carlosheitor.agencia.preco.CalculoReserva;
import dev.carlosheitor.agencia.reserva.dto.CriarReservaRequest;
import dev.carlosheitor.agencia.reserva.dto.ReservaResponse;
import dev.carlosheitor.agencia.saida.Saida;
import dev.carlosheitor.agencia.saida.SaidaService;

@Service
@Transactional(readOnly = true)
public class ReservaService {

	private final ReservaRepository repository;
	private final ClienteService clienteService;
	private final SaidaService saidaService;
	private final CalculadoraPreco calculadora;
	private final GeradorCodigoReserva geradorCodigo;
	private final Clock clock;

	public ReservaService(ReservaRepository repository, ClienteService clienteService, SaidaService saidaService,
			CalculadoraPreco calculadora, GeradorCodigoReserva geradorCodigo, Clock clock) {
		this.repository = repository;
		this.clienteService = clienteService;
		this.saidaService = saidaService;
		this.calculadora = calculadora;
		this.geradorCodigo = geradorCodigo;
		this.clock = clock;
	}

	/**
	 * A saída é lida com lock pessimista: reservas simultâneas na mesma saída entram uma de cada vez,
	 * e cada uma vê as vagas já descontadas pela anterior. Reservas em saídas diferentes não se bloqueiam.
	 */
	@Transactional
	public ReservaResponse criar(CriarReservaRequest request) {
		Cliente cliente = clienteService.obterEntidade(request.clienteId());
		Saida saida = saidaService.obterEntidadeComLock(request.saidaId());
		exigirSaidaVendavel(saida);

		CalculoReserva calculo = calculadora.calcularReserva(saida.getPacote().getPrecoBase(), request.qtdPassageiros());
		Reserva reserva = Reserva.criar(geradorCodigo.gerar(), cliente, saida, request.qtdPassageiros(), calculo,
				clock.instant());
		return ReservaResponse.from(repository.save(reserva));
	}

	public ReservaResponse buscar(String codigo) {
		return ReservaResponse.from(obterPorCodigo(codigo));
	}

	/**
	 * Ordem fixa dos locks: SAÍDA, depois RESERVA. Toda operação que trava as duas segue essa ordem,
	 * então duas transações nunca ficam esperando uma pela outra em ciclo (deadlock).
	 * A reserva é lida só depois dos locks: o status checado em reserva.cancelar() é o atual, e um
	 * segundo cancelamento simultâneo encontra CANCELADA e é recusado.
	 */
	@Transactional
	public ReservaResponse cancelar(String codigo) {
		Long saidaId = repository.findSaidaIdByCodigo(codigo)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Reserva", codigo));
		saidaService.obterEntidadeComLock(saidaId);
		Reserva reserva = repository.findByCodigoForUpdate(codigo)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Reserva", codigo));

		reserva.cancelar(clock.instant());
		return ReservaResponse.from(reserva);
	}

	private void exigirSaidaVendavel(Saida saida) {
		if (!saida.getPacote().isAtivo()) {
			throw new OperacaoInvalidaException("O pacote %s não está à venda".formatted(saida.getPacote().getNome()));
		}
		if (saida.jaPartiu(LocalDate.now(clock))) {
			throw new OperacaoInvalidaException("A saída de %s já partiu".formatted(saida.getDataPartida()));
		}
	}

	private Reserva obterPorCodigo(String codigo) {
		return repository.findByCodigo(codigo).orElseThrow(() -> new RecursoNaoEncontradoException("Reserva", codigo));
	}

}
