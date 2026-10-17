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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Pagamentos", description = "Pagamentos da reserva (gateway simulado)")
@RestController
@RequestMapping("/api")
public class PagamentoController {

	private final PagamentoService service;

	public PagamentoController(PagamentoService service) {
		this.service = service;
	}

	@Operation(summary = "Registra um pagamento (pendente) para a reserva",
			description = "Aceita vários pagamentos (ex.: sinal + saldo). Pagamento pendente ocupa saldo; "
					+ "409 saldo-excedido se passar do que falta pagar.")
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
	@Operation(summary = "Simula a aprovação do gateway",
			description = "Quando os pagamentos aprovados cobrem o total, a reserva vira CONFIRMADA.")
	@PostMapping("/pagamentos/{id}/aprovacao")
	public PagamentoResponse aprovar(@PathVariable Long id) {
		return service.aprovar(id);
	}

	/** Simula o webhook do gateway avisando que o pagamento foi recusado. */
	@Operation(summary = "Simula a recusa do gateway", description = "O valor recusado volta a ficar disponível no saldo.")
	@PostMapping("/pagamentos/{id}/recusa")
	public PagamentoResponse recusar(@PathVariable Long id) {
		return service.recusar(id);
	}

}
