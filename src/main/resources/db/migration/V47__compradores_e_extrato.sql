CREATE TABLE comprador (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenant(id), versao bigint,
 nome varchar(150) NOT NULL, telefone varchar(150), email varchar(150), observacoes varchar(500), arquivado boolean NOT NULL DEFAULT false
);
CREATE INDEX idx_comprador_busca ON comprador(tenant_id,arquivado,lower(nome));
CREATE TABLE movimentacao (
 id uuid PRIMARY KEY,tenant_id uuid NOT NULL REFERENCES tenant(id),tipo varchar(40) NOT NULL,descricao varchar(180) NOT NULL,
 quantidade numeric(18,6) NOT NULL,medida varchar(8) NOT NULL,referencia uuid,origem varchar(30) NOT NULL,data timestamp NOT NULL,usuario_id uuid
);
CREATE INDEX idx_movimentacao_data ON movimentacao(tenant_id,data DESC,id);
INSERT INTO movimentacao(id,tenant_id,tipo,descricao,quantidade,medida,referencia,origem,data)
 SELECT gen_random_uuid(),tenant_id,'SALDO_INICIAL',left(nome_fil || ' · ' || cor_fil,180),estoque_gramas,'g',id,'filamento',CURRENT_TIMESTAMP
 FROM filamento WHERE estoque_gramas<>0;
