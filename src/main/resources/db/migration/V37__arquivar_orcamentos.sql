ALTER TABLE pedido_orcamento ADD COLUMN arquivado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_pedido_arquivo ON pedido_orcamento(tenant_id,arquivado,criado_em DESC);
