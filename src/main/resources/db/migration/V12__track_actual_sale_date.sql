ALTER TABLE calculo_historico
    ADD COLUMN data_venda_hist TIMESTAMP NULL;

-- Para vendas antigas, a data do cálculo é a única referência histórica disponível.
UPDATE calculo_historico
SET data_venda_hist = data_hist
WHERE vendido_hist = TRUE;

CREATE INDEX idx_calculo_historico_usuario_data_venda
    ON calculo_historico (usuario_id, data_venda_hist)
    WHERE vendido_hist = TRUE;
