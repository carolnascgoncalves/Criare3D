ALTER TABLE venda_produto_loja ADD COLUMN quantidade_embalagens integer;
ALTER TABLE venda_produto_loja ADD CONSTRAINT ck_venda_quantidade_embalagens CHECK (quantidade_embalagens IS NULL OR quantidade_embalagens BETWEEN 0 AND 10000);
ALTER TABLE pedido_orcamento ADD COLUMN email varchar(150);
UPDATE pedido_orcamento SET email=contato, contato=NULL WHERE contato LIKE '%@%';
