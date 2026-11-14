CREATE TABLE operacao_confirmada (
 tenant_id uuid NOT NULL REFERENCES tenant(id), chave uuid NOT NULL, resumo varchar(64) NOT NULL,
 resultado varchar(150), criado_em timestamp NOT NULL, PRIMARY KEY(tenant_id,chave)
);
