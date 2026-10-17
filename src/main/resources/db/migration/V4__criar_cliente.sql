CREATE TABLE cliente (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome      VARCHAR(150) NOT NULL,
    -- CPF só com dígitos: a normalização acontece antes de gravar, então a UNIQUE pega "com e sem máscara"
    documento VARCHAR(11)  NOT NULL CHECK (documento ~ '^[0-9]{11}$'),
    email     VARCHAR(254) NOT NULL,
    telefone  VARCHAR(20)  NOT NULL,

    CONSTRAINT uk_cliente_documento UNIQUE (documento)
);
