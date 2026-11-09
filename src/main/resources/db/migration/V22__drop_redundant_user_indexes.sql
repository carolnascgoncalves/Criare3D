-- The Hibernate tenant filter and tenant-scoped indexes now provide isolation.
-- Keep usuario_id columns for attribution and account administration.
DROP INDEX IF EXISTS idx_peca_usuario;
DROP INDEX IF EXISTS idx_filamento_usuario;
DROP INDEX IF EXISTS idx_impressora_usuario;
DROP INDEX IF EXISTS idx_embalagem_usuario;
DROP INDEX IF EXISTS idx_configuracao_usuario;
DROP INDEX IF EXISTS idx_calculo_historico_usuario;
DROP INDEX IF EXISTS idx_calculo_historico_usuario_data;
DROP INDEX IF EXISTS idx_calculo_historico_usuario_data_venda;
