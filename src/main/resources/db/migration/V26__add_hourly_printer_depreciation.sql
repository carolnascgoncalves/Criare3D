ALTER TABLE impressora
    ADD COLUMN depreciacao_hora_imp NUMERIC(12, 8),
    ADD COLUMN depreciacao_origem_imp VARCHAR(30),
    ADD COLUMN anuncios_novos_imp INTEGER,
    ADD COLUMN anuncios_usados_imp INTEGER,
    ADD COLUMN depreciacao_manual_imp BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN horas_mensais_uso_imp NUMERIC(10, 2),
    ADD COLUMN horas_mensais_manual_imp BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE calculo_historico
    ADD COLUMN depreciacao_hora_impressora_hist NUMERIC(12, 8),
    ADD COLUMN depreciacao_origem_impressora_hist VARCHAR(30),
    ADD COLUMN anuncios_novos_depreciacao_hist INTEGER,
    ADD COLUMN anuncios_usados_depreciacao_hist INTEGER;
