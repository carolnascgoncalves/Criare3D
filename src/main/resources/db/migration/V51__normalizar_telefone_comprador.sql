ALTER TABLE comprador ADD COLUMN telefone_digitos varchar(150) NOT NULL DEFAULT '';

UPDATE comprador
SET telefone_digitos = CASE
    WHEN length(numero) > 11 AND left(numero, 2) = '55' THEN substring(numero FROM 3)
    ELSE numero
END
FROM (
    SELECT id, regexp_replace(coalesce(telefone, ''), '[^0-9]', '', 'g') AS numero
    FROM comprador
) normalizado
WHERE comprador.id = normalizado.id;

CREATE INDEX idx_comprador_telefone_digitos ON comprador(tenant_id, telefone_digitos);
