package dev.carlosheitor.agencia.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	boolean existsByDocumento(String documento);

}
