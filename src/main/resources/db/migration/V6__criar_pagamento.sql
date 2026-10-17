CREATE TABLE pagamento (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id      BIGINT        NOT NULL REFERENCES reserva (id),
    -- valor abate do saldo da reserva; valor_cobrado = valor + taxa de parcelamento
    valor           NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    forma           VARCHAR(20)   NOT NULL CHECK (forma IN ('PIX', 'CARTAO', 'BOLETO')),
    -- o máximo de parcelas é configurável (agencia.preco.maximo-parcelas), então o banco só exige >= 1
    parcelas        INTEGER       NOT NULL CHECK (parcelas >= 1),
    taxa_percentual NUMERIC(5,4)  NOT NULL,
    valor_cobrado   NUMERIC(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'ESTORNADO')),
    criado_em       TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_pagamento_reserva ON pagamento (reserva_id);
