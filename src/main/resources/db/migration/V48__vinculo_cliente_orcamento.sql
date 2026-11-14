ALTER TABLE comprador ADD CONSTRAINT uq_comprador_tenant_id UNIQUE(tenant_id,id);
ALTER TABLE pedido_orcamento ADD COLUMN comprador_id uuid;
ALTER TABLE pedido_orcamento ADD CONSTRAINT fk_pedido_comprador_empresa FOREIGN KEY(tenant_id,comprador_id) REFERENCES comprador(tenant_id,id);
CREATE INDEX idx_pedido_comprador ON pedido_orcamento(tenant_id,comprador_id,criado_em DESC);
