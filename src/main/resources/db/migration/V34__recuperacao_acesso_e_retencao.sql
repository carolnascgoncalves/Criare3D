-- Mudanças novas; migrations já aplicadas permanecem imutáveis.
ALTER TABLE tenant ADD COLUMN excluido_em TIMESTAMPTZ;
CREATE INDEX idx_tenant_excluido_em ON tenant (excluido_em) WHERE excluido_em IS NOT NULL;

CREATE TABLE recuperacao_senha (
    token_hash VARCHAR(64) PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    expira_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_recuperacao_usuario ON recuperacao_senha(usuario_id);
CREATE INDEX idx_recuperacao_expiracao ON recuperacao_senha(expira_em);

CREATE TABLE foto_rascunho (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    tipo VARCHAR(30) NOT NULL,
    bytes BYTEA NOT NULL CHECK (octet_length(bytes) <= 3145728),
    atualizado_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_foto_rascunho_tenant ON foto_rascunho(tenant_id);
CREATE INDEX idx_foto_rascunho_expiracao ON foto_rascunho(atualizado_em);
