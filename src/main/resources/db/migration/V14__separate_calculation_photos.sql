CREATE TABLE calculo_foto (
    calculo_id UUID PRIMARY KEY REFERENCES calculo_historico(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    bytes BYTEA NOT NULL,
    miniatura BYTEA
);

INSERT INTO calculo_foto (calculo_id, tipo, bytes)
SELECT id,
       COALESCE(NULLIF(tipo_foto_peca_hist, ''), 'image/jpeg'),
       decode(foto_peca_hist, 'base64')
FROM calculo_historico
WHERE foto_peca_hist IS NOT NULL AND foto_peca_hist <> '';

ALTER TABLE calculo_historico
    ADD COLUMN tem_foto_hist BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE calculo_historico
SET tem_foto_hist = TRUE
WHERE foto_peca_hist IS NOT NULL AND foto_peca_hist <> '';
