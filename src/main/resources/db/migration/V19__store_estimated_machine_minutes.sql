ALTER TABLE calculo_historico
    ADD COLUMN minutos_maquina_lote_hist BIGINT;

-- Mantém a estimativa que foi usada nos custos dos históricos já salvos.
UPDATE calculo_historico
SET minutos_maquina_lote_hist =
    (temp_hist + COALESCE(tempo_aquecimento_hist, 0)) * fornadas_hist;

ALTER TABLE calculo_historico
    ALTER COLUMN minutos_maquina_lote_hist SET DEFAULT 0,
    ALTER COLUMN minutos_maquina_lote_hist SET NOT NULL,
    ADD CONSTRAINT chk_calculo_historico_minutos_maquina
        CHECK (minutos_maquina_lote_hist >= 0);
