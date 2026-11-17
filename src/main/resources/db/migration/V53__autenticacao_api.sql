CREATE TABLE api_sessao (
    id uuid PRIMARY KEY,
    usuario_id uuid NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tenant_id uuid REFERENCES tenant(id),
    criado_em timestamptz NOT NULL,
    expira_em timestamptz NOT NULL,
    revogado_em timestamptz,
    UNIQUE (id, usuario_id),
    CHECK (expira_em > criado_em)
);
CREATE INDEX idx_api_sessao_usuario ON api_sessao(usuario_id);
CREATE INDEX idx_api_sessao_expiracao ON api_sessao(expira_em);

CREATE TABLE api_refresh_token (
    token_hash varchar(64) PRIMARY KEY,
    usuario_id uuid NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tenant_id uuid REFERENCES tenant(id),
    familia_id uuid NOT NULL,
    credencial_hash varchar(64) NOT NULL,
    criado_em timestamptz NOT NULL,
    expira_em timestamptz NOT NULL,
    revogado_em timestamptz,
    FOREIGN KEY (familia_id, usuario_id) REFERENCES api_sessao(id, usuario_id) ON DELETE CASCADE,
    CHECK (expira_em > criado_em)
);
CREATE INDEX idx_api_refresh_familia ON api_refresh_token(familia_id, usuario_id);
CREATE INDEX idx_api_refresh_expiracao ON api_refresh_token(expira_em);
