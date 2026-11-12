CREATE TABLE foto_item_pedido (
 item_id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), tipo VARCHAR(30) NOT NULL,
 bytes BYTEA NOT NULL CHECK(octet_length(bytes)<=3145728),
 FOREIGN KEY(tenant_id,item_id) REFERENCES item_pedido(tenant_id,id) ON DELETE CASCADE
);
CREATE INDEX idx_foto_item_pedido_tenant ON foto_item_pedido(tenant_id);
