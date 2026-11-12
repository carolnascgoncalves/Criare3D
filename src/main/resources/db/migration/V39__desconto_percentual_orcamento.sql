ALTER TABLE pedido_orcamento ADD COLUMN desconto_percentual NUMERIC(5,2) CHECK(desconto_percentual BETWEEN 0 AND 100);
