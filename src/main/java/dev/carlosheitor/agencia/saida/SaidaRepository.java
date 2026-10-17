package dev.carlosheitor.agencia.saida;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaidaRepository extends JpaRepository<Saida, Long> {

	boolean existsByPacoteIdAndDataPartida(Long pacoteId, LocalDate dataPartida);

	List<Saida> findByPacoteIdOrderByDataPartida(Long pacoteId);

}
