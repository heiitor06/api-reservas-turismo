package dev.carlosheitor.agencia.saida;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface SaidaRepository extends JpaRepository<Saida, Long> {

	boolean existsByPacoteIdAndDataPartida(Long pacoteId, LocalDate dataPartida);

	List<Saida> findByPacoteIdOrderByDataPartida(Long pacoteId);

	/**
	 * SELECT ... FOR UPDATE: trava a linha da saída até o fim da transação.
	 * Quem chegar depois espera na fila (até 3 s) e, quando entra, lê as vagas já atualizadas.
	 * Precisa ser a primeira leitura da saída na transação: se a entidade já estiver no contexto do JPA,
	 * o Hibernate trava a linha mas devolve a cópia antiga da memória.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
	@Query("select s from Saida s where s.id = :id")
	Optional<Saida> findByIdForUpdate(Long id);

}
