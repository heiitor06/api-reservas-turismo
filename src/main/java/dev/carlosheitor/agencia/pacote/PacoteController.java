package dev.carlosheitor.agencia.pacote;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.carlosheitor.agencia.pacote.dto.ItemRoteiroRequest;
import dev.carlosheitor.agencia.pacote.dto.PacoteRequest;
import dev.carlosheitor.agencia.pacote.dto.PacoteResponse;
import dev.carlosheitor.agencia.pacote.dto.PacoteResumoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Pacotes", description = "Pacotes turísticos e seus roteiros")
@RestController
@RequestMapping("/api/pacotes")
public class PacoteController {

	private final PacoteService service;

	public PacoteController(PacoteService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<PacoteResponse> criar(@Valid @RequestBody PacoteRequest request, UriComponentsBuilder uri) {
		PacoteResponse criado = service.criar(request);
		return ResponseEntity.created(localDoPacote(uri, criado.id())).body(criado);
	}

	@GetMapping
	public Page<PacoteResumoResponse> listar(@RequestParam(required = false) Long destinoId,
			@RequestParam(required = false) Boolean ativo, @PageableDefault(sort = "nome") Pageable pageable) {
		return service.listar(destinoId, ativo, pageable);
	}

	@GetMapping("/{id}")
	public PacoteResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PutMapping("/{id}")
	public PacoteResponse atualizar(@PathVariable Long id, @Valid @RequestBody PacoteRequest request) {
		return service.atualizar(id, request);
	}

	@PostMapping("/{id}/itens")
	public ResponseEntity<PacoteResponse> adicionarItem(@PathVariable Long id,
			@Valid @RequestBody ItemRoteiroRequest request, UriComponentsBuilder uri) {
		return ResponseEntity.created(localDoPacote(uri, id)).body(service.adicionarItem(id, request));
	}

	@DeleteMapping("/{id}/itens/{itemId}")
	public ResponseEntity<Void> removerItem(@PathVariable Long id, @PathVariable Long itemId) {
		service.removerItem(id, itemId);
		return ResponseEntity.noContent().build();
	}

	private static URI localDoPacote(UriComponentsBuilder uri, Long id) {
		return uri.path("/api/pacotes/{id}").buildAndExpand(id).toUri();
	}

}
