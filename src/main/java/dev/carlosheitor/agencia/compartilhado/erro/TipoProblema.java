package dev.carlosheitor.agencia.compartilhado.erro;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Catálogo dos problemas que a API devolve. Cada um vira um ProblemDetail (RFC 9457)
 * com type estável, para o cliente da API tratar o erro pelo type e não pelo texto.
 */
public enum TipoProblema {

	DADOS_INVALIDOS(HttpStatus.BAD_REQUEST, "dados-invalidos", "Dados inválidos"),
	RECURSO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "recurso-nao-encontrado", "Recurso não encontrado"),
	OPERACAO_INVALIDA(HttpStatus.CONFLICT, "operacao-invalida", "Operação inválida"),
	RECURSO_DUPLICADO(HttpStatus.CONFLICT, "recurso-duplicado", "Recurso duplicado"),
	ERRO_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno");

	private static final String BASE_TYPE = "https://api.agencia.local/problemas/";

	private final HttpStatus status;
	private final URI type;
	private final String titulo;

	TipoProblema(HttpStatus status, String slug, String titulo) {
		this.status = status;
		this.type = URI.create(BASE_TYPE + slug);
		this.titulo = titulo;
	}

	public HttpStatus status() {
		return status;
	}

	public ProblemDetail problema(String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setType(type);
		problema.setTitle(titulo);
		return problema;
	}

}
