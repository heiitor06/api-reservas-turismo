package dev.carlosheitor.agencia.destino;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.carlosheitor.agencia.compartilhado.erro.RecursoNaoEncontradoException;
import dev.carlosheitor.agencia.destino.dto.DestinoRequest;
import dev.carlosheitor.agencia.destino.dto.DestinoResponse;

@Service
@Transactional(readOnly = true)
public class DestinoService {

	private final DestinoRepository repository;

	public DestinoService(DestinoRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public DestinoResponse criar(DestinoRequest request) {
		Destino destino = new Destino(request.nome(), request.cidade(), request.uf(), request.pais(), request.descricao());
		return DestinoResponse.from(repository.save(destino));
	}

	public Page<DestinoResponse> listar(Pageable pageable) {
		return repository.findAll(pageable).map(DestinoResponse::from);
	}

	public DestinoResponse buscar(Long id) {
		return DestinoResponse.from(obterEntidade(id));
	}

	@Transactional
	public DestinoResponse atualizar(Long id, DestinoRequest request) {
		Destino destino = obterEntidade(id);
		destino.atualizar(request.nome(), request.cidade(), request.uf(), request.pais(), request.descricao());
		return DestinoResponse.from(destino);
	}

	/** Para outros services que precisam da entidade (ex.: criar um pacote num destino). */
	public Destino obterEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Destino", id));
	}

}
