package dev.carlosheitor.agencia.reserva;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	Optional<Reserva> findByCodigo(String codigo);

}
