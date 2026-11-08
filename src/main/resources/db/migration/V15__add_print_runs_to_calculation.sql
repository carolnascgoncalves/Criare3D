ALTER TABLE calculo_historico
    ADD COLUMN fornadas_hist INTEGER NOT NULL DEFAULT 1;

-- Os cálculos antigos tratavam cada unidade como uma impressão separada.
-- Preservamos essa interpretação ao migrar os dados já existentes.
UPDATE calculo_historico
SET fornadas_hist = unidades_hist;

ALTER TABLE calculo_historico
    ADD CONSTRAINT chk_calculo_historico_fornadas
        CHECK (fornadas_hist BETWEEN 1 AND unidades_hist);
