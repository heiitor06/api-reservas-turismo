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
	 * VERSÃO INGÊNUA (de propósito): lê a saída sem lock. Duas transações simultâneas leem o mesmo
	 * número de vagas, as duas passam na regra e as duas gravam: overbooking.
	 * O ConcorrenciaReservaIT prova isso; a correção vem na próxima etapa.
	 */
	@Transactional
	public ReservaResponse criar(CriarReservaRequest request) {
		Cliente cliente = clienteService.obterEntidade(request.clienteId());
		Saida saida = saidaService.obterEntidade(request.saidaId());
		exigirSaidaVendavel(saida);

		CalculoReserva calculo = calculadora.calcularReserva(saida.getPacote().getPrecoBase(), request.qtdPassageiros());
		Reserva reserva = Reserva.criar(geradorCodigo.gerar(), cliente, saida, request.qtdPassageiros(), calculo,
				clock.instant());
		return ReservaResponse.from(repository.save(reserva));
	}

	public ReservaResponse buscar(String codigo) {
		return ReservaResponse.from(obterPorCodigo(codigo));
	}

	@Transactional
	public ReservaResponse cancelar(String codigo) {
		Reserva reserva = obterPorCodigo(codigo);
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
