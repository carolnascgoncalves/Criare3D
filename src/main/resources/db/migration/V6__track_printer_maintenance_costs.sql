CREATE TABLE manutencao_impressora (
    id UUID PRIMARY KEY,
    descricao VARCHAR(150) NOT NULL,
    valor NUMERIC(10, 2) NOT NULL,
    data DATE NOT NULL,
    impressora_id UUID NOT NULL REFERENCES impressora(id)
);

CREATE INDEX idx_manutencao_impressora_data
    ON manutencao_impressora (impressora_id, data DESC);
