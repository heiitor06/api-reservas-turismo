package dev.carlosheitor.agencia.support;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Zera todas as tabelas da aplicação (mantém o histórico do Flyway).
 * TRUNCATE em vez de rollback: os testes de concorrência abrem transações em várias threads,
 * e um @Transactional no teste esconderia exatamente o que a gente quer provar.
 */
public class LimpezaBanco {

	private final JdbcTemplate jdbc;

	public LimpezaBanco(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void limpar() {
		List<String> tabelas = jdbc.queryForList("""
				select tablename from pg_tables
				where schemaname = 'public' and tablename <> 'flyway_schema_history'
				""", String.class);
		if (tabelas.isEmpty()) {
			return;
		}
		jdbc.execute("TRUNCATE TABLE " + String.join(", ", tabelas) + " RESTART IDENTITY CASCADE");
	}

}
