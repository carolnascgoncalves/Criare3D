ALTER TABLE configuracao
    DROP COLUMN IF EXISTS valor_impressora,
    DROP COLUMN IF EXISTS vida_util_impressora_horas;

ALTER TABLE peca
    ADD COLUMN tempo_trabalho_minutos BIGINT NOT NULL DEFAULT 0;

ALTER TABLE impressora
    ADD COLUMN valor_compra_imp NUMERIC(10, 2),
    ADD COLUMN vida_util_horas_imp NUMERIC(10, 2),
    ADD COLUMN manutencao_hora_imp NUMERIC(10, 2);

ALTER TABLE calculo_historico
    DROP COLUMN IF EXISTS valor_impressora_hist,
    ADD COLUMN tempo_trabalho_minutos_hist BIGINT,
    ADD COLUMN valor_compra_impressora_hist NUMERIC(10, 2),
    ADD COLUMN manutencao_hora_impressora_hist NUMERIC(10, 2);
