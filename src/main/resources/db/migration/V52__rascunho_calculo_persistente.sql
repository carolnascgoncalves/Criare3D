CREATE TABLE calculo_rascunho (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenant(id),
    usuario_id uuid NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    json jsonb NOT NULL DEFAULT '{}',
    atualizado_em timestamptz NOT NULL,
    versao bigint NOT NULL DEFAULT 0,
    UNIQUE (id, tenant_id, usuario_id)
);
CREATE INDEX idx_calculo_rascunho_usuario ON calculo_rascunho(tenant_id, usuario_id, atualizado_em DESC);
CREATE INDEX idx_calculo_rascunho_expiracao ON calculo_rascunho(atualizado_em);

-- Uma foto por cálculo, sem substituir a foto de outra aba do mesmo usuário.
ALTER TABLE foto_rascunho DROP CONSTRAINT foto_rascunho_usuario_id_key;
ALTER TABLE foto_rascunho ADD COLUMN rascunho_id uuid UNIQUE;
ALTER TABLE foto_rascunho ADD CONSTRAINT fk_foto_rascunho_calculo
    FOREIGN KEY (rascunho_id, tenant_id, usuario_id)
    REFERENCES calculo_rascunho(id, tenant_id, usuario_id) ON DELETE CASCADE;
