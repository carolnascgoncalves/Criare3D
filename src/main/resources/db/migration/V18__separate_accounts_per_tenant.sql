-- Cada cliente corresponde a uma conta USER e tem identidade visual própria.
-- Contas USER que chegaram a compartilhar um tenant são separadas abaixo.
-- O dono dos dados é resolvido por usuario_id; os dados são movidos junto
-- com a conta, e tema/logo atuais são copiados como ponto de partida.

CREATE TEMPORARY TABLE _tenant_conta_separar ON COMMIT DROP AS
SELECT usuario_id,
       tenant_origem_id,
       usuario_id AS tenant_destino_id
FROM (
    SELECT u.id AS usuario_id,
           u.tenant_id AS tenant_origem_id,
           ROW_NUMBER() OVER (PARTITION BY u.tenant_id ORDER BY u.id) AS ordem
    FROM usuario u
    WHERE u.perfil = 'USER'
      AND u.tenant_id IS NOT NULL
) contas
WHERE ordem > 1;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM _tenant_conta_separar s
        JOIN tenant t ON t.id = s.tenant_destino_id
    ) THEN
        RAISE EXCEPTION 'V18 cancelada: um ID de usuário já está sendo usado como ID de tenant.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM _tenant_conta_separar s
        JOIN usuario u ON u.id = s.usuario_id
        JOIN tenant t ON t.slug = 'cliente-' || replace(s.usuario_id::text, '-', '')
    ) THEN
        RAISE EXCEPTION 'V18 cancelada: identificador gerado para separar contas já está em uso.';
    END IF;
END $$;

INSERT INTO tenant (id, nome, slug, ativo, cor_primaria, cor_destaque, cor_fundo, logo, tema_versao)
SELECT s.tenant_destino_id,
       LEFT('Cliente ' || u.email, 120),
       'cliente-' || replace(s.usuario_id::text, '-', ''),
       origem.ativo,
       origem.cor_primaria,
       origem.cor_destaque,
       origem.cor_fundo,
       origem.logo,
       origem.tema_versao
FROM _tenant_conta_separar s
JOIN usuario u ON u.id = s.usuario_id
JOIN tenant origem ON origem.id = s.tenant_origem_id;

-- As FKs compostas precisam ser recriadas depois da troca coordenada dos IDs.
ALTER TABLE peca DROP CONSTRAINT fk_peca_tenant_usuario;
ALTER TABLE filamento DROP CONSTRAINT fk_filamento_tenant_usuario;
ALTER TABLE impressora DROP CONSTRAINT fk_impressora_tenant_usuario;
ALTER TABLE embalagem DROP CONSTRAINT fk_embalagem_tenant_usuario;
ALTER TABLE configuracao DROP CONSTRAINT fk_configuracao_tenant_usuario;
ALTER TABLE calculo_historico DROP CONSTRAINT fk_calculo_historico_tenant_usuario;
ALTER TABLE calculo_historico DROP CONSTRAINT fk_calculo_tenant_filamento;
ALTER TABLE calculo_historico DROP CONSTRAINT fk_calculo_tenant_impressora;
ALTER TABLE calculo_historico DROP CONSTRAINT fk_calculo_tenant_embalagem;
ALTER TABLE calculo_historico DROP CONSTRAINT fk_calculo_tenant_peca;
ALTER TABLE manutencao_impressora DROP CONSTRAINT fk_manutencao_tenant_impressora;
ALTER TABLE item_calculo_historico DROP CONSTRAINT fk_item_tenant_calculo;
ALTER TABLE item_calculo_historico DROP CONSTRAINT fk_item_tenant_filamento;
ALTER TABLE calculo_foto DROP CONSTRAINT fk_foto_tenant_calculo;

UPDATE usuario u
SET tenant_id = s.tenant_destino_id
FROM _tenant_conta_separar s
WHERE u.id = s.usuario_id;

UPDATE peca p
SET tenant_id = s.tenant_destino_id
FROM _tenant_conta_separar s
WHERE p.usuario_id = s.usuario_id;

UPDATE filamento f
SET tenant_id = s.tenant_destino_id
FROM _tenant_conta_separar s
WHERE f.usuario_id = s.usuario_id;

UPDATE impressora i
SET tenant_id = s.tenant_destino_id
FROM _tenant_conta_separar s
WHERE i.usuario_id = s.usuario_id;

UPDATE embalagem e
SET tenant_id = s.tenant_destino_id
FROM _tenant_conta_separar s
WHERE e.usuario_id = s.usuario_id;

UPDATE configuracao c
SET tenant_id = s.tenant_destino_id
FROM _tenant_conta_separar s
WHERE c.usuario_id = s.usuario_id;

UPDATE calculo_historico h
SET tenant_id = s.tenant_destino_id
FROM _tenant_conta_separar s
WHERE h.usuario_id = s.usuario_id;

-- Registros filhos acompanham o tenant do respectivo registro pai.
UPDATE manutencao_impressora m
SET tenant_id = i.tenant_id
FROM impressora i
WHERE i.id = m.impressora_id
  AND m.tenant_id IS DISTINCT FROM i.tenant_id;

UPDATE item_calculo_historico item
SET tenant_id = h.tenant_id
FROM calculo_historico h
WHERE h.id = item.id_calculo_fk
  AND item.tenant_id IS DISTINCT FROM h.tenant_id;

UPDATE calculo_foto foto
SET tenant_id = h.tenant_id
FROM calculo_historico h
WHERE h.id = foto.calculo_id
  AND foto.tenant_id IS DISTINCT FROM h.tenant_id;

ALTER TABLE peca
    ADD CONSTRAINT fk_peca_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE filamento
    ADD CONSTRAINT fk_filamento_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE impressora
    ADD CONSTRAINT fk_impressora_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE embalagem
    ADD CONSTRAINT fk_embalagem_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE configuracao
    ADD CONSTRAINT fk_configuracao_tenant_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES usuario (tenant_id, id);
ALTER TABLE calculo_historico
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

-- O painel e a identidade visual tratam uma conta USER por tenant.
CREATE UNIQUE INDEX uq_usuario_user_por_tenant
    ON usuario (tenant_id)
    WHERE perfil = 'USER' AND tenant_id IS NOT NULL;
