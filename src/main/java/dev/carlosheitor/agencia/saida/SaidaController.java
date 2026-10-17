package dev.carlosheitor.agencia.saida;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.carlosheitor.agencia.saida.dto.AlterarStatusSaidaRequest;
import dev.carlosheitor.agencia.saida.dto.SaidaRequest;
import dev.carlosheitor.agencia.saida.dto.SaidaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Saídas", description = "Datas de partida de cada pacote, com lotação")
@RestController
@RequestMapping("/api")
public class SaidaController {

	private final SaidaService service;

	public SaidaController(SaidaService service) {
		this.service = service;
	}

	@PostMapping("/pacotes/{pacoteId}/saidas")
	public ResponseEntity<SaidaResponse> criar(@PathVariable Long pacoteId, @Valid @RequestBody SaidaRequest request,
			UriComponentsBuilder uri) {
		SaidaResponse criada = service.criar(pacoteId, request);
		URI location = uri.path("/api/saidas/{id}").buildAndExpand(criada.id()).toUri();
		return ResponseEntity.created(location).body(criada);
	}

	@GetMapping("/pacotes/{pacoteId}/saidas")
	public List<SaidaResponse> listarPorPacote(@PathVariable Long pacoteId) {
		return service.listarPorPacote(pacoteId);
	}

	@GetMapping("/saidas/{id}")
	public SaidaResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PatchMapping("/saidas/{id}/status")
	public SaidaResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusSaidaRequest request) {
		return service.alterarStatus(id, request.status());
	}

}
