ALTER TABLE impressora
    ADD COLUMN tempo_aquecimento_min_imp INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_impressora_tempo_aquecimento
        CHECK (tempo_aquecimento_min_imp BETWEEN 0 AND 10000);

ALTER TABLE configuracao
    ADD COLUMN purga_gramas_por_impressao NUMERIC(12, 6) NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_configuracao_purga_gramas
        CHECK (purga_gramas_por_impressao >= 0);

ALTER TABLE calculo_historico
    ADD COLUMN pecas_por_impressao_hist INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN tempo_aquecimento_hist INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN purga_gramas_hist NUMERIC(12, 6) NOT NULL DEFAULT 0,
    ADD COLUMN custo_purga_hist NUMERIC(16, 8) NOT NULL DEFAULT 0,
    ADD COLUMN gramas_totais_lote_hist NUMERIC(14, 6) NOT NULL DEFAULT 0;

UPDATE calculo_historico
SET pecas_por_impressao_hist = GREATEST(
    1,
    CEIL(unidades_hist::NUMERIC / GREATEST(fornadas_hist, 1))::INTEGER
);

ALTER TABLE calculo_historico
    ADD CONSTRAINT chk_calculo_historico_pecas_por_impressao
        CHECK (pecas_por_impressao_hist BETWEEN 1 AND 10000),
    ADD CONSTRAINT chk_calculo_historico_aquecimento
        CHECK (tempo_aquecimento_hist BETWEEN 0 AND 10000),
    ADD CONSTRAINT chk_calculo_historico_purga
        CHECK (purga_gramas_hist >= 0 AND custo_purga_hist >= 0);

ALTER TABLE item_calculo_historico
    ADD COLUMN purga_gramas_item NUMERIC(12, 6) NOT NULL DEFAULT 0,
    ALTER COLUMN quantidade_item TYPE NUMERIC(12, 6),
    ALTER COLUMN custo_filamento_item TYPE NUMERIC(16, 8);

UPDATE calculo_historico h
SET gramas_totais_lote_hist = COALESCE((
    SELECT SUM(i.quantidade_item + i.purga_gramas_item) * h.unidades_hist
    FROM item_calculo_historico i WHERE i.id_calculo_fk = h.id
), h.qntd_fil_hist * h.unidades_hist);

ALTER TABLE calculo_historico
    ALTER COLUMN custo_fil_hist TYPE NUMERIC(16, 8),
    ALTER COLUMN custo_energia_hist TYPE NUMERIC(16, 8),
    ALTER COLUMN custo_emb_hist TYPE NUMERIC(16, 8),
    ALTER COLUMN custo_total_hist TYPE NUMERIC(16, 8),
    ALTER COLUMN qntd_fil_hist TYPE NUMERIC(12, 6),
    ALTER COLUMN custo_mao_obra_hist TYPE NUMERIC(16, 8),
    ALTER COLUMN custo_depreciacao_hist TYPE NUMERIC(16, 8),
    ALTER COLUMN custo_manutencao_hist TYPE NUMERIC(16, 8),
    ALTER COLUMN custo_falha_hist TYPE NUMERIC(16, 8);

CREATE INDEX IF NOT EXISTS idx_item_calculo_historico_calculo
    ON item_calculo_historico (id_calculo_fk);

ALTER TABLE filamento
    ALTER COLUMN estoque_gramas TYPE NUMERIC(12, 6);

CREATE INDEX IF NOT EXISTS idx_calculo_historico_usuario_data
    ON calculo_historico (usuario_id, data_hist DESC);
