package dev.carlosheitor.agencia.preco;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.carlosheitor.agencia.preco.dto.CotacaoRequest;
import dev.carlosheitor.agencia.preco.dto.CotacaoResponse;
import dev.carlosheitor.agencia.saida.Saida;
import dev.carlosheitor.agencia.saida.SaidaService;

/** Simula o preço sem reservar: não trava nem valida vagas, só informa quantas há agora. */
@Service
@Transactional(readOnly = true)
public class CotacaoService {

	private final SaidaService saidaService;
	private final CalculadoraPreco calculadora;

	public CotacaoService(SaidaService saidaService, CalculadoraPreco calculadora) {
		this.saidaService = saidaService;
		this.calculadora = calculadora;
	}

	public CotacaoResponse cotar(CotacaoRequest request) {
		Saida saida = saidaService.obterEntidade(request.saidaId());
		CalculoReserva calculo = calculadora.calcularReserva(saida.getPacote().getPrecoBase(), request.qtdPassageiros());
		BigDecimal taxa = calculadora.taxaParcelamento(request.forma(), request.parcelas());
		BigDecimal valorCobrado = calculadora.valorCobrado(calculo.valorTotal(), taxa);

		return new CotacaoResponse(
				request.qtdPassageiros(),
				calculo.valorUnitario(),
				calculo.subtotal(),
				calculo.percentualDesconto(),
				calculo.valorDesconto(),
				calculo.valorTotal(),
				request.forma(),
				request.parcelas(),
				taxa,
				valorCobrado,
				calculadora.valorParcela(valorCobrado, request.parcelas()),
				saida.getVagasDisponiveis());
	}

}
