ALTER TABLE configuracao
    ADD COLUMN mao_obra_ativada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN valor_mao_obra_hora NUMERIC(10, 2),
    ADD COLUMN depreciacao_ativada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN valor_impressora NUMERIC(10, 2),
    ADD COLUMN vida_util_impressora_horas NUMERIC(10, 2),
    ADD COLUMN taxa_falha_ativada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN percentual_falha NUMERIC(5, 2);

ALTER TABLE calculo_historico
    ADD COLUMN custo_mao_obra_hist NUMERIC(10, 2),
    ADD COLUMN custo_depreciacao_hist NUMERIC(10, 2),
    ADD COLUMN custo_falha_hist NUMERIC(10, 2),
    ADD COLUMN mao_obra_ativada_hist BOOLEAN,
    ADD COLUMN valor_mao_obra_hora_hist NUMERIC(10, 2),
    ADD COLUMN depreciacao_ativada_hist BOOLEAN,
    ADD COLUMN valor_impressora_hist NUMERIC(10, 2),
    ADD COLUMN vida_util_impressora_horas_hist NUMERIC(10, 2),
    ADD COLUMN taxa_falha_ativada_hist BOOLEAN,
    ADD COLUMN percentual_falha_hist NUMERIC(5, 2);
