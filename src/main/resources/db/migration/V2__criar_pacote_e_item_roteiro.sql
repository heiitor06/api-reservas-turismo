CREATE TABLE pacote (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    destino_id   BIGINT        NOT NULL REFERENCES destino (id),
    nome         VARCHAR(150)  NOT NULL,
    descricao    TEXT,
    duracao_dias INTEGER       NOT NULL CHECK (duracao_dias > 0),
    preco_base   NUMERIC(12,2) NOT NULL CHECK (preco_base > 0),
    ativo        BOOLEAN       NOT NULL
);

-- Postgres não cria índice em FK sozinho; a listagem filtra por destino
CREATE INDEX idx_pacote_destino ON pacote (destino_id);

CREATE TABLE item_roteiro (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pacote_id BIGINT       NOT NULL REFERENCES pacote (id) ON DELETE CASCADE,
    tipo      VARCHAR(20)  NOT NULL CHECK (tipo IN ('HOTEL', 'TRANSPORTE', 'PASSEIO')),
    titulo    VARCHAR(150) NOT NULL,
    descricao TEXT,
    dia       INTEGER      NOT NULL CHECK (dia > 0),
    ordem     INTEGER      NOT NULL CHECK (ordem >= 0)
);

CREATE INDEX idx_item_roteiro_pacote ON item_roteiro (pacote_id);
