package dev.carlosheitor.agencia.pagamento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

	List<Pagamento> findByReservaIdOrderByCriadoEmAscIdAsc(Long reservaId);

	/** Só o id da reserva, sem carregar o pagamento: serve para travar a reserva antes de ler o pagamento. */
	@Query("select p.reserva.id from Pagamento p where p.id = :pagamentoId")
	Optional<Long> findReservaIdByPagamentoId(Long pagamentoId);

}
