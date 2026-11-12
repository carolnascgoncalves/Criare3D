CREATE TABLE falha_producao (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), calculo_id UUID NOT NULL,
 filamento_id UUID NOT NULL, unidades INTEGER NOT NULL CHECK(unidades > 0), gramas NUMERIC(12,6) NOT NULL CHECK(gramas > 0),
 minutos BIGINT NOT NULL CHECK(minutos >= 0), custo_material NUMERIC(16,8) NOT NULL, observacao VARCHAR(500),
 data_registro TIMESTAMP NOT NULL, cancelada BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(tenant_id, calculo_id) REFERENCES calculo_historico(tenant_id,id),
 FOREIGN KEY(tenant_id, filamento_id) REFERENCES filamento(tenant_id,id));
CREATE INDEX idx_falha_calculo ON falha_producao(tenant_id,calculo_id);
CREATE TABLE planejamento_producao (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), calculo_id UUID NOT NULL, impressora_id UUID NOT NULL,
 nome_peca VARCHAR(150) NOT NULL, nome_impressora VARCHAR(150) NOT NULL, unidades INTEGER NOT NULL CHECK(unidades > 0),
 pecas_por_impressao INTEGER NOT NULL CHECK(pecas_por_impressao > 0), intervalo INTEGER NOT NULL CHECK(intervalo >= 0),
 folga INTEGER NOT NULL CHECK(folga BETWEEN 0 AND 100), inicio TIMESTAMP NOT NULL, fim TIMESTAMP NOT NULL,
 cancelado BOOLEAN NOT NULL DEFAULT FALSE, CHECK(fim > inicio),
 FOREIGN KEY(tenant_id,calculo_id) REFERENCES calculo_historico(tenant_id,id),
 FOREIGN KEY(tenant_id,impressora_id) REFERENCES impressora(tenant_id,id));
CREATE INDEX idx_planejamento_impressora ON planejamento_producao(tenant_id,impressora_id,fim) WHERE cancelado = FALSE;
