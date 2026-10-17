package dev.carlosheitor.agencia.preco;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.carlosheitor.agencia.preco.dto.CotacaoRequest;
import dev.carlosheitor.agencia.preco.dto.CotacaoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** POST e não GET: a cotação recebe um corpo com vários parâmetros, mas não cria nada (responde 200). */
@Tag(name = "Cotações", description = "Simulação de preço sem reservar")
@RestController
@RequestMapping("/api/cotacoes")
public class CotacaoController {

	private final CotacaoService service;

	public CotacaoController(CotacaoService service) {
		this.service = service;
	}

	@PostMapping
	public CotacaoResponse cotar(@Valid @RequestBody CotacaoRequest request) {
		return service.cotar(request);
	}

}
