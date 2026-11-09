-- Introduz o modelo de organização sem remover usuario_id.
-- Os defaults mantêm compatibilidade com o código anterior até a Fase 2.

DO $$
DECLARE
    proprietarios_distintos BIGINT;
    usuarios_cliente BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM peca WHERE usuario_id IS NULL)
       OR EXISTS (SELECT 1 FROM filamento WHERE usuario_id IS NULL)
       OR EXISTS (SELECT 1 FROM impressora WHERE usuario_id IS NULL)
       OR EXISTS (SELECT 1 FROM embalagem WHERE usuario_id IS NULL)
       OR EXISTS (SELECT 1 FROM configuracao WHERE usuario_id IS NULL)
       OR EXISTS (SELECT 1 FROM calculo_historico WHERE usuario_id IS NULL) THEN
        RAISE EXCEPTION 'V17 cancelada: existem registros sem usuario_id nas tabelas principais. Defina o destino desses dados antes de migrar.';
    END IF;

    SELECT COUNT(DISTINCT proprietario_id)
      INTO proprietarios_distintos
      FROM (
          SELECT usuario_id AS proprietario_id FROM peca
          UNION ALL SELECT usuario_id FROM filamento
          UNION ALL SELECT usuario_id FROM impressora
          UNION ALL SELECT usuario_id FROM embalagem
          UNION ALL SELECT usuario_id FROM configuracao
          UNION ALL SELECT usuario_id FROM calculo_historico
      ) proprietarios;

    IF proprietarios_distintos > 1 THEN
        RAISE EXCEPTION 'V17 cancelada: há registros pertencentes a mais de uma conta. Mapeie cada conta para sua organização antes de migrar; a migration não vai juntar os dados.';
    END IF;

    SELECT COUNT(*) INTO usuarios_cliente FROM usuario WHERE perfil = 'USER';
    IF usuarios_cliente > 1 THEN
        RAISE EXCEPTION 'V17 cancelada: há mais de uma conta USER. Associe cada conta à organização correta antes de migrar; a migration não vai juntar contas.';
    END IF;
END $$;

CREATE TABLE tenant (
    id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    cor_primaria VARCHAR(7) NOT NULL,
    cor_destaque VARCHAR(7) NOT NULL,
    cor_fundo VARCHAR(7) NOT NULL,
    logo BYTEA,
    tema_versao BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT tenant_pkey PRIMARY KEY (id),
    CONSTRAINT tenant_slug_unique UNIQUE (slug),
    CONSTRAINT chk_tenant_tema_versao CHECK (tema_versao > 0),
    CONSTRAINT chk_tenant_cor_primaria CHECK (cor_primaria ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_tenant_cor_destaque CHECK (cor_destaque ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_tenant_cor_fundo CHECK (cor_fundo ~ '^#[0-9A-Fa-f]{6}$')
);

-- Cores atuais do Criare 3D: marrom, rosa e fundo bege.
INSERT INTO tenant (id, nome, slug, ativo, cor_primaria, cor_destaque, cor_fundo, tema_versao)
VALUES ('a14d3f70-17a0-4e1b-8a14-000000000017', 'Criare 3D', 'criare3d', TRUE,
        '#492e1e', '#e9a695', '#f5e7da', 1);

ALTER TABLE usuario ADD COLUMN tenant_id UUID;
UPDATE usuario
   SET tenant_id = 'a14d3f70-17a0-4e1b-8a14-000000000017';
ALTER TABLE usuario
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ADD CONSTRAINT fk_usuario_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);

ALTER TABLE peca ADD COLUMN tenant_id UUID;
ALTER TABLE filamento ADD COLUMN tenant_id UUID;
ALTER TABLE impressora ADD COLUMN tenant_id UUID;
ALTER TABLE embalagem ADD COLUMN tenant_id UUID;
ALTER TABLE configuracao ADD COLUMN tenant_id UUID;
ALTER TABLE calculo_historico ADD COLUMN tenant_id UUID;

UPDATE peca p SET tenant_id = u.tenant_id FROM usuario u WHERE u.id = p.usuario_id;
UPDATE filamento f SET tenant_id = u.tenant_id FROM usuario u WHERE u.id = f.usuario_id;
UPDATE impressora i SET tenant_id = u.tenant_id FROM usuario u WHERE u.id = i.usuario_id;
UPDATE embalagem e SET tenant_id = u.tenant_id FROM usuario u WHERE u.id = e.usuario_id;
UPDATE configuracao c SET tenant_id = u.tenant_id FROM usuario u WHERE u.id = c.usuario_id;
UPDATE calculo_historico h SET tenant_id = u.tenant_id FROM usuario u WHERE u.id = h.usuario_id;

-- Os registros dependentes recebem a organização do pai, nunca uma associação independente.
ALTER TABLE manutencao_impressora ADD COLUMN tenant_id UUID;
ALTER TABLE item_calculo_historico ADD COLUMN tenant_id UUID;
ALTER TABLE calculo_foto ADD COLUMN tenant_id UUID;

UPDATE manutencao_impressora m
   SET tenant_id = i.tenant_id
  FROM impressora i
 WHERE i.id = m.impressora_id;

UPDATE item_calculo_historico item
   SET tenant_id = h.tenant_id
  FROM calculo_historico h
 WHERE h.id = item.id_calculo_fk;

UPDATE calculo_foto foto
   SET tenant_id = h.tenant_id
  FROM calculo_historico h
 WHERE h.id = foto.calculo_id;

-- Valores padrão preservam inserts feitos pela versão anterior da aplicação.
ALTER TABLE peca
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_peca_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE filamento
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_filamento_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE impressora
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_impressora_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE embalagem
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_embalagem_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE configuracao
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_configuracao_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE calculo_historico
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_calculo_historico_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE manutencao_impressora
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_manutencao_impressora_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE item_calculo_historico
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_item_calculo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE calculo_foto
    ALTER COLUMN tenant_id SET DEFAULT 'a14d3f70-17a0-4e1b-8a14-000000000017',
    ALTER COLUMN tenant_id SET NOT NULL,
    ADD CONSTRAINT fk_calculo_foto_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);

-- Chaves compostas impedem que relações futuras cruzem organizações.
ALTER TABLE usuario
    ADD CONSTRAINT uk_usuario_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE peca
    ADD CONSTRAINT uk_peca_tenant_id UNIQUE (tenant_id, id),
    ADD CONSTRAINT fk_peca_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE filamento
    ADD CONSTRAINT uk_filamento_tenant_id UNIQUE (tenant_id, id),
    ADD CONSTRAINT fk_filamento_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE impressora
    ADD CONSTRAINT uk_impressora_tenant_id UNIQUE (tenant_id, id),
    ADD CONSTRAINT fk_impressora_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE embalagem
    ADD CONSTRAINT uk_embalagem_tenant_id UNIQUE (tenant_id, id),
    ADD CONSTRAINT fk_embalagem_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE configuracao
    ADD CONSTRAINT uk_configuracao_tenant_id UNIQUE (tenant_id, id),
    ADD CONSTRAINT fk_configuracao_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE calculo_historico
    ADD CONSTRAINT uk_calculo_historico_tenant_id UNIQUE (tenant_id, id),
    ADD CONSTRAINT fk_calculo_historico_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id),
    ADD CONSTRAINT fk_calculo_tenant_filamento FOREIGN KEY (tenant_id, id_filamento_fk) REFERENCES filamento (tenant_id, id),
    ADD CONSTRAINT fk_calculo_tenant_impressora FOREIGN KEY (tenant_id, id_impressora_fk) REFERENCES impressora (tenant_id, id),
    ADD CONSTRAINT fk_calculo_tenant_embalagem FOREIGN KEY (tenant_id, id_embalagem_fk) REFERENCES embalagem (tenant_id, id),
    ADD CONSTRAINT fk_calculo_tenant_peca FOREIGN KEY (tenant_id, id_peca_fk) REFERENCES peca (tenant_id, id);
ALTER TABLE manutencao_impressora
    ADD CONSTRAINT fk_manutencao_tenant_impressora FOREIGN KEY (tenant_id, impressora_id) REFERENCES impressora (tenant_id, id);
ALTER TABLE item_calculo_historico
    ADD CONSTRAINT fk_item_tenant_calculo FOREIGN KEY (tenant_id, id_calculo_fk) REFERENCES calculo_historico (tenant_id, id),
    ADD CONSTRAINT fk_item_tenant_filamento FOREIGN KEY (tenant_id, id_filamento_fk) REFERENCES filamento (tenant_id, id);
ALTER TABLE calculo_foto
    ADD CONSTRAINT fk_foto_tenant_calculo FOREIGN KEY (tenant_id, calculo_id) REFERENCES calculo_historico (tenant_id, id);

-- Mantemos os índices por usuario_id durante a transição; o código atual ainda os usa.
CREATE INDEX idx_usuario_tenant ON usuario (tenant_id);
CREATE INDEX idx_peca_tenant ON peca (tenant_id);
CREATE INDEX idx_filamento_tenant ON filamento (tenant_id);
CREATE INDEX idx_impressora_tenant ON impressora (tenant_id);
CREATE INDEX idx_embalagem_tenant ON embalagem (tenant_id);
CREATE INDEX idx_configuracao_tenant ON configuracao (tenant_id);
CREATE INDEX idx_calculo_historico_tenant_data ON calculo_historico (tenant_id, data_hist DESC);
CREATE INDEX idx_calculo_historico_tenant_data_venda ON calculo_historico (tenant_id, data_venda_hist);
CREATE INDEX idx_manutencao_impressora_tenant_data
    ON manutencao_impressora (tenant_id, impressora_id, data DESC);
CREATE INDEX idx_item_calculo_historico_tenant_calculo
    ON item_calculo_historico (tenant_id, id_calculo_fk);
CREATE INDEX idx_calculo_foto_tenant_calculo
    ON calculo_foto (tenant_id, calculo_id);
