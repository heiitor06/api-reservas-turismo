package dev.carlosheitor.agencia.reserva;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	Optional<Reserva> findByCodigo(String codigo);

	/** Só o id da saída, sem carregar a reserva: serve para travar a saída antes de ler a reserva. */
	@Query("select r.saida.id from Reserva r where r.codigo = :codigo")
	Optional<Long> findSaidaIdByCodigo(String codigo);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
	@Query("select r from Reserva r where r.codigo = :codigo")
	Optional<Reserva> findByCodigoForUpdate(String codigo);

}
