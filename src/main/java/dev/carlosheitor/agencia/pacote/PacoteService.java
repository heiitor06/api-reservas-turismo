package dev.carlosheitor.agencia.pacote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.carlosheitor.agencia.compartilhado.erro.RecursoNaoEncontradoException;
import dev.carlosheitor.agencia.destino.Destino;
import dev.carlosheitor.agencia.destino.DestinoService;
import dev.carlosheitor.agencia.pacote.dto.ItemRoteiroRequest;
import dev.carlosheitor.agencia.pacote.dto.PacoteRequest;
import dev.carlosheitor.agencia.pacote.dto.PacoteResponse;
import dev.carlosheitor.agencia.pacote.dto.PacoteResumoResponse;

@Service
@Transactional(readOnly = true)
public class PacoteService {

	private final PacoteRepository repository;
	private final DestinoService destinoService;

	public PacoteService(PacoteRepository repository, DestinoService destinoService) {
		this.repository = repository;
		this.destinoService = destinoService;
	}

	@Transactional
	public PacoteResponse criar(PacoteRequest request) {
		Destino destino = destinoService.obterEntidade(request.destinoId());
		Pacote pacote = new Pacote(destino, request.nome(), request.descricao(), request.duracaoDias(),
				request.precoBase(), request.ativo());
		return PacoteResponse.from(repository.save(pacote));
	}

	public Page<PacoteResumoResponse> listar(Long destinoId, Boolean ativo, Pageable pageable) {
		return repository.buscar(destinoId, ativo, pageable).map(PacoteResumoResponse::from);
	}

	/** Mapeia para DTO dentro da transação: com open-in-view desligado, itens e destino (LAZY) só carregam aqui. */
	public PacoteResponse buscar(Long id) {
		return PacoteResponse.from(obterEntidade(id));
	}

	@Transactional
	public PacoteResponse atualizar(Long id, PacoteRequest request) {
		Pacote pacote = obterEntidade(id);
		Destino destino = destinoService.obterEntidade(request.destinoId());
		pacote.atualizar(destino, request.nome(), request.descricao(), request.duracaoDias(), request.precoBase(),
				request.ativo());
		return PacoteResponse.from(pacote);
	}

	@Transactional
	public PacoteResponse adicionarItem(Long pacoteId, ItemRoteiroRequest request) {
		Pacote pacote = obterEntidade(pacoteId);
		pacote.adicionarItem(request.tipo(), request.titulo(), request.descricao(), request.dia(), request.ordem());
		// força o INSERT agora para o item novo já sair com id na resposta
		repository.flush();
		return PacoteResponse.from(pacote);
	}

	@Transactional
	public void removerItem(Long pacoteId, Long itemId) {
		obterEntidade(pacoteId).removerItem(itemId);
	}

	public Pacote obterEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pacote", id));
	}

}
