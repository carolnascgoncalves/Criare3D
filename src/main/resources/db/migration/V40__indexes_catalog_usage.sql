-- Consultas de uso por conta e cadastro, sem alterar migrations já aplicadas.
CREATE INDEX idx_calculo_tenant_filamento ON calculo_historico (tenant_id, id_filamento_fk);
CREATE INDEX idx_calculo_tenant_impressora_produzido ON calculo_historico (tenant_id, id_impressora_fk) WHERE produzido_hist = TRUE;
CREATE INDEX idx_calculo_tenant_peca ON calculo_historico (tenant_id, id_peca_fk);
CREATE INDEX idx_item_tenant_filamento ON item_calculo_historico (tenant_id, id_filamento_fk);
