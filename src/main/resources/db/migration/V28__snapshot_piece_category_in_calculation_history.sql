ALTER TABLE calculo_historico
    ADD COLUMN categoria_peca_hist VARCHAR(60);

UPDATE calculo_historico ch
   SET categoria_peca_hist = p.categoria
  FROM peca p
 WHERE ch.id_peca_fk = p.id
   AND ch.tenant_id = p.tenant_id;
