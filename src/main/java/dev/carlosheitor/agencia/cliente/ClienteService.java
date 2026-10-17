package dev.carlosheitor.agencia.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.carlosheitor.agencia.cliente.dto.ClienteRequest;
import dev.carlosheitor.agencia.cliente.dto.ClienteResponse;
import dev.carlosheitor.agencia.compartilhado.erro.RecursoDuplicadoException;
import dev.carlosheitor.agencia.compartilhado.erro.RecursoNaoEncontradoException;

@Service
@Transactional(readOnly = true)
public class ClienteService {

	private final ClienteRepository repository;

	public ClienteService(ClienteRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public ClienteResponse criar(ClienteRequest request) {
		Cliente cliente = new Cliente(request.nome(), request.documento(), request.email(), request.telefone());
		if (repository.existsByDocumento(cliente.getDocumento())) {
			throw new RecursoDuplicadoException("Já existe cliente com o CPF " + cliente.getDocumentoMascarado());
		}
		return ClienteResponse.from(repository.save(cliente));
	}

	public Page<ClienteResponse> listar(Pageable pageable) {
		return repository.findAll(pageable).map(ClienteResponse::from);
	}

	public ClienteResponse buscar(Long id) {
		return ClienteResponse.from(obterEntidade(id));
	}

	public Cliente obterEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
	}

}
