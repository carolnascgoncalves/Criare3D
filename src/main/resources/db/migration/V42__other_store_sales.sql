CREATE TABLE produto_loja (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    versao BIGINT,
    nome VARCHAR(150) NOT NULL,
    preco NUMERIC(12,2) NOT NULL CHECK(preco >= 0),
    custo NUMERIC(12,2) CHECK(custo >= 0),
    arquivado BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(tenant_id,id)
);
CREATE TABLE venda_produto_loja (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    versao BIGINT,
    produto_id UUID NOT NULL,
    nome_produto VARCHAR(150) NOT NULL,
    quantidade INTEGER NOT NULL CHECK(quantidade > 0),
    canceladas INTEGER NOT NULL DEFAULT 0 CHECK(canceladas >= 0 AND canceladas <= quantidade),
    preco_unitario NUMERIC(12,2) NOT NULL CHECK(preco_unitario >= 0),
    custo_unitario NUMERIC(12,2) CHECK(custo_unitario >= 0),
    data_venda TIMESTAMP NOT NULL,
    data_cancelamento TIMESTAMP,
    FOREIGN KEY(tenant_id,produto_id) REFERENCES produto_loja(tenant_id,id)
);
CREATE INDEX idx_produto_loja_nome ON produto_loja(tenant_id,arquivado,nome);
CREATE INDEX idx_venda_produto_loja_data ON venda_produto_loja(tenant_id,data_venda);
