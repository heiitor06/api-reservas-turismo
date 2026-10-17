package dev.carlosheitor.agencia.pagamento;

import java.util.EnumSet;
import java.util.Set;

public enum StatusPagamento {

	PENDENTE, APROVADO, RECUSADO, ESTORNADO;

	/** O que já foi pago de fato: conta para confirmar a reserva. */
	public static final Set<StatusPagamento> APROVADOS = EnumSet.of(APROVADO);

	/** O que ocupa saldo: um pendente "segura" o valor até ser aprovado ou recusado. */
	public static final Set<StatusPagamento> COMPROMETIDOS = EnumSet.of(PENDENTE, APROVADO);

}
