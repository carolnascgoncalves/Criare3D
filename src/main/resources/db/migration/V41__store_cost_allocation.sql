CREATE TABLE custo_loja (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    descricao VARCHAR(150) NOT NULL,
    inicio DATE NOT NULL,
    meta NUMERIC(12,2) NOT NULL CHECK (meta > 0),
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('VALOR_UNIDADE','PERCENTUAL','RATEIO_MENSAL')),
    valor NUMERIC(12,2) NOT NULL CHECK (valor >= 0),
    unidades_previstas INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (tenant_id,id),
    CHECK (tipo <> 'RATEIO_MENSAL' OR unidades_previstas > 0)
);
ALTER TABLE calculo_historico ADD COLUMN custo_loja_hist NUMERIC(16,8) NOT NULL DEFAULT 0;
CREATE TABLE rateio_custo_historico (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    calculo_id UUID NOT NULL,
    custo_loja_id UUID NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    valor_unitario NUMERIC(16,8) NOT NULL CHECK (valor_unitario >= 0),
    FOREIGN KEY (tenant_id,calculo_id) REFERENCES calculo_historico(tenant_id,id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id,custo_loja_id) REFERENCES custo_loja(tenant_id,id),
    UNIQUE(tenant_id,calculo_id,custo_loja_id)
);
CREATE INDEX idx_rateio_meta ON rateio_custo_historico(tenant_id,custo_loja_id);
CREATE INDEX idx_custo_loja_ativo ON custo_loja(tenant_id,ativo,inicio);
