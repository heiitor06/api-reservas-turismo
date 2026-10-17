CREATE TABLE destino (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome      VARCHAR(120) NOT NULL,
    cidade    VARCHAR(120) NOT NULL,
    uf        VARCHAR(2),
    pais      VARCHAR(60)  NOT NULL,
    descricao TEXT
);
