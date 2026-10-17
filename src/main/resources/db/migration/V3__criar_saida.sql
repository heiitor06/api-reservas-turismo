CREATE TABLE saida (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pacote_id         BIGINT      NOT NULL REFERENCES pacote (id),
    data_partida      DATE        NOT NULL,
    capacidade_total  INTEGER     NOT NULL CHECK (capacidade_total > 0),
    vagas_disponiveis INTEGER     NOT NULL,
    status            VARCHAR(20) NOT NULL CHECK (status IN ('ABERTA', 'ENCERRADA', 'CANCELADA')),

    -- Rede de segurança: mesmo com bug no código, o banco nunca grava vaga negativa ou acima da capacidade
    CONSTRAINT ck_saida_vagas CHECK (vagas_disponiveis BETWEEN 0 AND capacidade_total),

    -- Também serve de índice para buscas por pacote_id (coluna líder)
    CONSTRAINT uk_saida_pacote_data UNIQUE (pacote_id, data_partida)
);
