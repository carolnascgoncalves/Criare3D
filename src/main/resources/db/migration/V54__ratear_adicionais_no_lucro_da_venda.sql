ALTER TABLE venda_calculo ALTER COLUMN lucro_unitario TYPE NUMERIC(24,8);

-- Reconstitui somente os adicionais ativos que já existiam na data da venda.
WITH adicionais AS (
    SELECT tenant_id, calculo_id, data_registro, valor
    FROM custo_personalizado WHERE NOT cancelado
    UNION ALL
    SELECT tenant_id, calculo_id, data_registro, custo_material
    FROM falha_producao WHERE NOT cancelada
), rateio AS (
    SELECT v.id, ROUND(SUM(a.valor) / h.unidades_hist, 8) AS adicional,
           COALESCE(v.lucro_unitario, h.lucro_liquido_hist, h.preco_sug_hist - h.custo_total_hist) AS lucro_base
    FROM venda_calculo v
    JOIN calculo_historico h ON h.id = v.calculo_id AND h.tenant_id = v.tenant_id
    JOIN adicionais a ON a.calculo_id = v.calculo_id AND a.tenant_id = v.tenant_id
                     AND a.data_registro <= v.data_venda
    WHERE h.unidades_hist > 0
    GROUP BY v.id, h.id
)
UPDATE venda_calculo v SET lucro_unitario = r.lucro_base - r.adicional
FROM rateio r WHERE v.id = r.id;
