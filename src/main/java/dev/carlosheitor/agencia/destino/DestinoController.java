package dev.carlosheitor.agencia.destino;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.carlosheitor.agencia.destino.dto.DestinoRequest;
import dev.carlosheitor.agencia.destino.dto.DestinoResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/destinos")
public class DestinoController {

	private final DestinoService service;

	public DestinoController(DestinoService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<DestinoResponse> criar(@Valid @RequestBody DestinoRequest request, UriComponentsBuilder uri) {
		DestinoResponse criado = service.criar(request);
		URI location = uri.path("/api/destinos/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(location).body(criado);
	}

	@GetMapping
	public Page<DestinoResponse> listar(@PageableDefault(sort = "nome") Pageable pageable) {
		return service.listar(pageable);
	}

	@GetMapping("/{id}")
	public DestinoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PutMapping("/{id}")
	public DestinoResponse atualizar(@PathVariable Long id, @Valid @RequestBody DestinoRequest request) {
		return service.atualizar(id, request);
	}

}
