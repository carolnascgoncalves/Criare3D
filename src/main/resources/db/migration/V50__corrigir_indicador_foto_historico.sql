-- Torna visíveis fotos já armazenadas cujo indicador não foi persistido.
UPDATE calculo_historico h SET tem_foto_hist=true
WHERE h.tem_foto_hist=false AND EXISTS (
    SELECT 1 FROM calculo_foto f
    WHERE f.calculo_id=h.id AND f.tenant_id=h.tenant_id AND octet_length(f.bytes)>0
);
