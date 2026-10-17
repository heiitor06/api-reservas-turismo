package dev.carlosheitor.agencia.cliente;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.carlosheitor.agencia.cliente.dto.ClienteRequest;
import dev.carlosheitor.agencia.cliente.dto.ClienteResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Clientes", description = "Cadastro de clientes (CPF mascarado nas respostas)")
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

	private final ClienteService service;

	public ClienteController(ClienteService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request, UriComponentsBuilder uri) {
		ClienteResponse criado = service.criar(request);
		URI location = uri.path("/api/clientes/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(location).body(criado);
	}

	@GetMapping
	public Page<ClienteResponse> listar(@PageableDefault(sort = "nome") Pageable pageable) {
		return service.listar(pageable);
	}

	@GetMapping("/{id}")
	public ClienteResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

}
