ALTER TABLE calculo_historico
    ADD COLUMN unidades_hist INTEGER NOT NULL DEFAULT 1;

ALTER TABLE calculo_historico
    ADD CONSTRAINT chk_calculo_historico_unidades
        CHECK (unidades_hist BETWEEN 1 AND 10000);
