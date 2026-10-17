package dev.carlosheitor.agencia.reserva;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.carlosheitor.agencia.reserva.dto.CriarReservaRequest;
import dev.carlosheitor.agencia.reserva.dto.ReservaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Reservas", description = "Reserva de vagas com controle de concorrência")
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

	private final ReservaService service;

	public ReservaController(ReservaService service) {
		this.service = service;
	}

	@Operation(summary = "Reserva vagas numa saída",
			description = "Debita as vagas na hora e grava o preço do momento. 409 vagas-insuficientes se não couber; "
					+ "503 tente-novamente se a saída estiver ocupada por outras reservas por mais de 3 s.")
	@PostMapping
	public ResponseEntity<ReservaResponse> criar(@Valid @RequestBody CriarReservaRequest request,
			UriComponentsBuilder uri) {
		ReservaResponse criada = service.criar(request);
		URI location = uri.path("/api/reservas/{codigo}").buildAndExpand(criada.codigo()).toUri();
		return ResponseEntity.created(location).body(criada);
	}

	@GetMapping("/{codigo}")
	public ReservaResponse buscar(@PathVariable String codigo) {
		return service.buscar(codigo);
	}

	/** Cancelamento como sub-recurso (POST), não DELETE: a reserva continua existindo, com status CANCELADA. */
	@Operation(summary = "Cancela a reserva e devolve as vagas", description = "409 se já estiver cancelada.")
	@PostMapping("/{codigo}/cancelamento")
	public ReservaResponse cancelar(@PathVariable String codigo) {
		return service.cancelar(codigo);
	}

}
