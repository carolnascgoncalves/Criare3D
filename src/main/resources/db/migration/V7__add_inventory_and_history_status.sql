ALTER TABLE filamento
    ADD COLUMN estoque_gramas NUMERIC(10, 2);

UPDATE filamento
SET estoque_gramas = peso_rolo_fil
WHERE estoque_gramas IS NULL;

ALTER TABLE filamento
    ALTER COLUMN estoque_gramas SET NOT NULL;

ALTER TABLE calculo_historico
    ADD COLUMN produzido_hist BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN vendido_hist BOOLEAN NOT NULL DEFAULT FALSE;
