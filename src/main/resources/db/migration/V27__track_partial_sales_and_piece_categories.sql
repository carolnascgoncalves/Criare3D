ALTER TABLE peca
    ADD COLUMN categoria VARCHAR(60);

ALTER TABLE calculo_historico
    ADD COLUMN unidades_vendidas_hist INTEGER NOT NULL DEFAULT 0;

UPDATE calculo_historico
   SET unidades_vendidas_hist = unidades_hist
 WHERE vendido_hist = TRUE;

ALTER TABLE calculo_historico
    ADD CONSTRAINT ck_calculo_unidades_vendidas
        CHECK (unidades_vendidas_hist >= 0 AND unidades_vendidas_hist <= unidades_hist);

CREATE TABLE venda_calculo (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    calculo_id UUID NOT NULL,
    unidades INTEGER NOT NULL CHECK (unidades > 0),
    data_venda TIMESTAMP NOT NULL
);

ALTER TABLE venda_calculo
    ADD CONSTRAINT fk_venda_calculo_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    ADD CONSTRAINT fk_venda_calculo_historico
        FOREIGN KEY (tenant_id, calculo_id) REFERENCES calculo_historico(tenant_id, id) ON DELETE CASCADE;

INSERT INTO venda_calculo (id, tenant_id, calculo_id, unidades, data_venda)
SELECT gen_random_uuid(), tenant_id, id, unidades_hist, COALESCE(data_venda_hist, data_hist)
  FROM calculo_historico
 WHERE vendido_hist = TRUE;

CREATE INDEX idx_venda_calculo_tenant_data ON venda_calculo (tenant_id, data_venda);
CREATE INDEX idx_venda_calculo_tenant_calculo ON venda_calculo (tenant_id, calculo_id);
