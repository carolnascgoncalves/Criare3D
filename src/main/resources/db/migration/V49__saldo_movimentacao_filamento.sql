ALTER TABLE movimentacao ADD COLUMN filamento_id uuid REFERENCES filamento(id) ON DELETE SET NULL;
ALTER TABLE movimentacao ADD COLUMN saldo_apos numeric(18,6);
CREATE INDEX idx_movimentacao_filamento ON movimentacao(tenant_id, filamento_id, data DESC, id);

-- Só há identificação segura nos registros cuja origem já era o próprio filamento.
-- Saldos antigos permanecem desconhecidos; não se reconstrói um saldo sem dados confiáveis.
UPDATE movimentacao m SET filamento_id=m.referencia
WHERE m.origem='filamento' AND EXISTS
    (SELECT 1 FROM filamento f WHERE f.id=m.referencia AND f.tenant_id=m.tenant_id);
