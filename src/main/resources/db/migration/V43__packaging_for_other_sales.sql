ALTER TABLE venda_produto_loja
    ADD COLUMN tipo_embalagem VARCHAR(20) NOT NULL DEFAULT 'SEM' CHECK(tipo_embalagem IN ('SEM','CADASTRADA','PERSONALIZADA')),
    ADD COLUMN embalagem_id UUID,
    ADD COLUMN nome_embalagem VARCHAR(150),
    ADD COLUMN custo_embalagem NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(custo_embalagem >= 0);
-- Nome e custo são snapshots: a alteração ou remoção do cadastro não muda a venda.
