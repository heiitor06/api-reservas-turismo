package dev.carlosheitor.agencia.saida;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.carlosheitor.agencia.compartilhado.erro.OperacaoInvalidaException;
import dev.carlosheitor.agencia.compartilhado.erro.RecursoDuplicadoException;
import dev.carlosheitor.agencia.compartilhado.erro.RecursoNaoEncontradoException;
import dev.carlosheitor.agencia.pacote.Pacote;
import dev.carlosheitor.agencia.pacote.PacoteService;
import dev.carlosheitor.agencia.saida.dto.SaidaRequest;
import dev.carlosheitor.agencia.saida.dto.SaidaResponse;

@Service
@Transactional(readOnly = true)
public class SaidaService {

	private final SaidaRepository repository;
	private final PacoteService pacoteService;
	private final Clock clock;

	public SaidaService(SaidaRepository repository, PacoteService pacoteService, Clock clock) {
		this.repository = repository;
		this.pacoteService = pacoteService;
		this.clock = clock;
	}

	/**
	 * A checagem de duplicidade dá a mensagem amigável no caso comum. Em requisições simultâneas
	 * as duas passam por ela, e quem segura é a UNIQUE do banco (tratada no GlobalExceptionHandler).
	 */
	@Transactional
	public SaidaResponse criar(Long pacoteId, SaidaRequest request) {
		Pacote pacote = pacoteService.obterEntidade(pacoteId);
		if (!request.dataPartida().isAfter(LocalDate.now(clock))) {
			throw new OperacaoInvalidaException("A data de partida precisa ser futura: " + request.dataPartida());
		}
		if (repository.existsByPacoteIdAndDataPartida(pacoteId, request.dataPartida())) {
			throw new RecursoDuplicadoException("O pacote %d já tem saída em %s".formatted(pacoteId, request.dataPartida()));
		}
		Saida saida = Saida.nova(pacote, request.dataPartida(), request.capacidadeTotal());
		return SaidaResponse.from(repository.save(saida));
	}

	public List<SaidaResponse> listarPorPacote(Long pacoteId) {
		pacoteService.obterEntidade(pacoteId);
		return repository.findByPacoteIdOrderByDataPartida(pacoteId).stream().map(SaidaResponse::from).toList();
	}

	public SaidaResponse buscar(Long id) {
		return SaidaResponse.from(obterEntidade(id));
	}

	@Transactional
	public SaidaResponse alterarStatus(Long id, StatusSaida novo) {
		Saida saida = obterEntidade(id);
		saida.alterarStatus(novo);
		return SaidaResponse.from(saida);
	}

	public Saida obterEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Saída", id));
	}

}
