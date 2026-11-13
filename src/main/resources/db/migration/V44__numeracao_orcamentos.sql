-- Stable display numbers; references and UUID links are unchanged.
CREATE SEQUENCE pedido_orcamento_numero_seq;
ALTER TABLE pedido_orcamento ADD COLUMN numero bigint;
WITH numerados AS (
 SELECT id, row_number() OVER (ORDER BY criado_em, id) AS numero FROM pedido_orcamento
)
UPDATE pedido_orcamento p SET numero = n.numero FROM numerados n WHERE p.id = n.id;
SELECT setval('pedido_orcamento_numero_seq', COALESCE((SELECT max(numero) FROM pedido_orcamento),0)+1, false);
ALTER TABLE pedido_orcamento ALTER COLUMN numero SET DEFAULT nextval('pedido_orcamento_numero_seq');
ALTER TABLE pedido_orcamento ALTER COLUMN numero SET NOT NULL;
CREATE UNIQUE INDEX uq_pedido_orcamento_numero ON pedido_orcamento(numero);
ALTER SEQUENCE pedido_orcamento_numero_seq OWNED BY pedido_orcamento.numero;
