-- Inserts devem informar explicitamente o tenant, evitando atribuir dados
-- de novas contas ao tenant inicial por omissão.
ALTER TABLE usuario
    ALTER COLUMN tenant_id DROP DEFAULT;

ALTER TABLE peca
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE filamento
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE impressora
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE embalagem
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE configuracao
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE calculo_historico
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE manutencao_impressora
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE item_calculo_historico
    ALTER COLUMN tenant_id DROP DEFAULT;
ALTER TABLE calculo_foto
    ALTER COLUMN tenant_id DROP DEFAULT;
