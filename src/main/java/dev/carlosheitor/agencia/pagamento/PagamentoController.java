package dev.carlosheitor.agencia.pagamento;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.carlosheitor.agencia.pagamento.dto.PagamentoResponse;
import dev.carlosheitor.agencia.pagamento.dto.RegistrarPagamentoRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PagamentoController {

	private final PagamentoService service;

	public PagamentoController(PagamentoService service) {
		this.service = service;
	}

	@PostMapping("/reservas/{codigo}/pagamentos")
	public ResponseEntity<PagamentoResponse> registrar(@PathVariable String codigo,
			@Valid @RequestBody RegistrarPagamentoRequest request, UriComponentsBuilder uri) {
		PagamentoResponse criado = service.registrar(codigo, request);
		URI location = uri.path("/api/pagamentos/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(location).body(criado);
	}

	@GetMapping("/pagamentos/{id}")
	public PagamentoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	/** Simula o webhook do gateway avisando que o pagamento foi aprovado. */
	@PostMapping("/pagamentos/{id}/aprovacao")
	public PagamentoResponse aprovar(@PathVariable Long id) {
		return service.aprovar(id);
	}

	/** Simula o webhook do gateway avisando que o pagamento foi recusado. */
	@PostMapping("/pagamentos/{id}/recusa")
	public PagamentoResponse recusar(@PathVariable Long id) {
		return service.recusar(id);
	}

}
