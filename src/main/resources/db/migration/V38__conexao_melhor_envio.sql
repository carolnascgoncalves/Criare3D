CREATE TABLE melhor_envio_conexao (
 tenant_id UUID NOT NULL REFERENCES tenant(id), ambiente VARCHAR(10) NOT NULL,
 acesso TEXT NOT NULL, renovacao TEXT NOT NULL, expira_em TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(tenant_id,ambiente)
);
