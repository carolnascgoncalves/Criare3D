ALTER TABLE calculo_historico ADD COLUMN data_producao_hist TIMESTAMP;
ALTER TABLE calculo_historico ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
-- A data dos lotes antigos permanece desconhecida; não substituímos pela data do cálculo.
ALTER TABLE venda_calculo ADD COLUMN unidades_canceladas INTEGER NOT NULL DEFAULT 0;
ALTER TABLE venda_calculo ADD COLUMN data_cancelamento TIMESTAMP;
ALTER TABLE venda_calculo ADD COLUMN preco_unitario NUMERIC(16,8);
ALTER TABLE venda_calculo ADD COLUMN desconto_unitario NUMERIC(16,8);
ALTER TABLE venda_calculo ADD COLUMN taxas_unitarias NUMERIC(16,8);
ALTER TABLE venda_calculo ADD COLUMN lucro_unitario NUMERIC(16,8);
ALTER TABLE venda_calculo ADD COLUMN valores_reais BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE venda_calculo ADD CONSTRAINT ck_venda_canceladas CHECK (unidades_canceladas >= 0 AND unidades_canceladas <= unidades);
ALTER TABLE peca ADD COLUMN arquivado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE filamento ADD COLUMN arquivado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE impressora ADD COLUMN arquivado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE embalagem ADD COLUMN arquivado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_historico_producao ON calculo_historico(tenant_id, data_producao_hist) WHERE produzido_hist = TRUE;
