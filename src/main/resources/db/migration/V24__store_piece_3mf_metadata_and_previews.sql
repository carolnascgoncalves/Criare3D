ALTER TABLE peca
    ADD COLUMN dados_3mf_json TEXT;

CREATE TABLE peca_arquivo_placa (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    peca_id UUID NOT NULL,
    numero SMALLINT NOT NULL,
    mime_type VARCHAR(30) NOT NULL,
    preview BYTEA NOT NULL,
    CONSTRAINT ck_peca_arquivo_placa_numero CHECK (numero BETWEEN 1 AND 8),
    CONSTRAINT uk_peca_arquivo_placa UNIQUE (tenant_id, peca_id, numero),
    CONSTRAINT fk_peca_arquivo_placa_peca
        FOREIGN KEY (tenant_id, peca_id) REFERENCES peca (tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX idx_peca_arquivo_placa_peca
    ON peca_arquivo_placa (tenant_id, peca_id, numero);
