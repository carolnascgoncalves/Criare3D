CREATE TABLE pedido_orcamento (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), versao BIGINT NOT NULL DEFAULT 0,
 revisao BIGINT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL, titulo VARCHAR(150) NOT NULL,
 cliente VARCHAR(150), contato VARCHAR(150), observacoes VARCHAR(1000), criado_em TIMESTAMP NOT NULL,
 enviado_em TIMESTAMP, aprovado_em TIMESTAMP, entregue_em TIMESTAMP, cancelado_em TIMESTAMP, pago_em TIMESTAMP,
 inicio_pretendido TIMESTAMP, previsao TIMESTAMP, turno_inicio TIME NOT NULL, turno_fim TIME NOT NULL,
 dias VARCHAR(13) NOT NULL, intervalo INTEGER NOT NULL CHECK(intervalo BETWEEN 0 AND 1440),
 folga INTEGER NOT NULL CHECK(folga BETWEEN 0 AND 100), desconto NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(desconto>=0),
 frete NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(frete>=0), custo_frete NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(custo_frete>=0),
 UNIQUE(tenant_id,id), CHECK(turno_fim>turno_inicio)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_planejamento_pedido_tenant_id ON planejamento_producao(tenant_id,id);
CREATE TABLE item_pedido (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), pedido_id UUID NOT NULL,
 ordem INTEGER NOT NULL, nome VARCHAR(150) NOT NULL, materiais VARCHAR(300), peca_id UUID, impressora_id UUID,
 entrada_json TEXT NOT NULL, quantidade INTEGER NOT NULL CHECK(quantidade BETWEEN 1 AND 500),
 usar_estoque INTEGER NOT NULL DEFAULT 0 CHECK(usar_estoque>=0 AND usar_estoque<=quantidade),
 reserva_estoque INTEGER NOT NULL DEFAULT 0 CHECK(reserva_estoque>=0 AND reserva_estoque<=usar_estoque),
 reserva_producao INTEGER NOT NULL DEFAULT 0 CHECK(reserva_producao>=0 AND reserva_producao<=quantidade-usar_estoque),
 lote_estoque_id UUID, lote_producao_id UUID, planejamento_id UUID,
 preco_unitario NUMERIC(16,8) NOT NULL CHECK(preco_unitario>0), taxas_unitarias NUMERIC(16,8) NOT NULL DEFAULT 0,
 custo NUMERIC(20,8) NOT NULL, minutos_producao BIGINT NOT NULL, aquecimento INTEGER NOT NULL DEFAULT 0,
 FOREIGN KEY(tenant_id,pedido_id) REFERENCES pedido_orcamento(tenant_id,id) ON DELETE CASCADE,
 FOREIGN KEY(tenant_id,peca_id) REFERENCES peca(tenant_id,id),
 FOREIGN KEY(tenant_id,impressora_id) REFERENCES impressora(tenant_id,id),
 FOREIGN KEY(tenant_id,lote_estoque_id) REFERENCES calculo_historico(tenant_id,id),
 FOREIGN KEY(tenant_id,lote_producao_id) REFERENCES calculo_historico(tenant_id,id),
 FOREIGN KEY(tenant_id,planejamento_id) REFERENCES planejamento_producao(tenant_id,id),
 UNIQUE(tenant_id,id)
);
CREATE INDEX idx_item_pedido_pedido ON item_pedido(tenant_id,pedido_id);
CREATE INDEX idx_item_pedido_estoque ON item_pedido(tenant_id,lote_estoque_id);
CREATE INDEX idx_item_pedido_producao ON item_pedido(tenant_id,lote_producao_id);
CREATE INDEX idx_pedido_lista ON pedido_orcamento(tenant_id,criado_em DESC);
ALTER TABLE venda_calculo ADD COLUMN pedido_id UUID;
ALTER TABLE venda_calculo ADD CONSTRAINT fk_venda_pedido_tenant FOREIGN KEY(tenant_id,pedido_id) REFERENCES pedido_orcamento(tenant_id,id);
CREATE INDEX idx_venda_pedido ON venda_calculo(tenant_id,pedido_id);
