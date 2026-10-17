package dev.carlosheitor.agencia.pacote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PacoteRepository extends JpaRepository<Pacote, Long> {

	/**
	 * Filtros opcionais: parâmetro nulo não filtra.
	 * O EntityGraph traz o destino no mesmo SELECT, evitando um SELECT extra por pacote (N+1) na listagem.
	 */
	@EntityGraph(attributePaths = "destino")
	@Query("""
			select p from Pacote p
			where (:destinoId is null or p.destino.id = :destinoId)
			  and (:ativo is null or p.ativo = :ativo)
			""")
	Page<Pacote> buscar(Long destinoId, Boolean ativo, Pageable pageable);

}
