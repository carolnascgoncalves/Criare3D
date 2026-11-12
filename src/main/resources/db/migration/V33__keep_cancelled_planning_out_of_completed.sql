-- A V32 já foi aplicada; ajustes posteriores pertencem a uma nova versão.
-- Planejamentos retirados da fila não devem também aparecer como concluídos.
UPDATE planejamento_producao
SET concluido = FALSE
WHERE cancelado = TRUE AND concluido = TRUE;
