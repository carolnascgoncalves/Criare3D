CREATE TABLE custo_personalizado (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    calculo_id UUID NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    valor NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    data_registro TIMESTAMP NOT NULL,
    cancelado BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (tenant_id, calculo_id) REFERENCES calculo_historico(tenant_id,id)
);
CREATE INDEX idx_custo_personalizado_calculo ON custo_personalizado(tenant_id,calculo_id);
ALTER TABLE planejamento_producao ADD COLUMN concluido BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE planejamento_producao p SET concluido = TRUE
FROM calculo_historico h
WHERE h.tenant_id = p.tenant_id AND h.id = p.calculo_id
  AND h.unidades_vendidas_hist >= h.unidades_hist;
