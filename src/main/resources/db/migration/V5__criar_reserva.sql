CREATE TABLE reserva (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo              VARCHAR(10)   NOT NULL,
    cliente_id          BIGINT        NOT NULL REFERENCES cliente (id),
    saida_id            BIGINT        NOT NULL REFERENCES saida (id),
    qtd_passageiros     INTEGER       NOT NULL CHECK (qtd_passageiros > 0),
    -- preço da época: mudar o preço do pacote depois não altera reservas já feitas
    valor_unitario      NUMERIC(12,2) NOT NULL,
    percentual_desconto NUMERIC(5,4)  NOT NULL,
    valor_total         NUMERIC(12,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'CANCELADA')),
    criada_em           TIMESTAMPTZ   NOT NULL,
    cancelada_em        TIMESTAMPTZ,

    CONSTRAINT uk_reserva_codigo UNIQUE (codigo)
);

CREATE INDEX idx_reserva_saida ON reserva (saida_id);
CREATE INDEX idx_reserva_cliente ON reserva (cliente_id);
