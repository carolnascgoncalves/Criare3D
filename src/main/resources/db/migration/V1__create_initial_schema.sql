-- Estrutura atual do Criare 3D para bancos novos.
-- Bancos existentes são registrados no Flyway na versão 1 e não executam este arquivo.

CREATE TABLE usuario (
    id UUID NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    CONSTRAINT usuario_pkey PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE peca (
    id UUID NOT NULL,
    nome_peca VARCHAR(150) NOT NULL,
    qnt_fil_peca NUMERIC(10, 2) NOT NULL,
    tmp_peca BIGINT NOT NULL,
    usuario_id UUID,
    CONSTRAINT peca_pkey PRIMARY KEY (id),
    CONSTRAINT fk_peca_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE filamento (
    id UUID NOT NULL,
    nome_fil VARCHAR(150) NOT NULL,
    cor_fil VARCHAR(80) NOT NULL,
    codigo_cor_fil VARCHAR(7),
    marca_fil VARCHAR(150),
    peso_rolo_fil NUMERIC(10, 2) NOT NULL,
    preco_rolo_fil NUMERIC(10, 2) NOT NULL,
    usuario_id UUID,
    CONSTRAINT filamento_pkey PRIMARY KEY (id),
    CONSTRAINT fk_filamento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE impressora (
    id UUID NOT NULL,
    nome_imp VARCHAR(150) NOT NULL,
    modelo_imp VARCHAR(150) NOT NULL,
    potencia_imp NUMERIC(10, 2) NOT NULL,
    usuario_id UUID,
    CONSTRAINT impressora_pkey PRIMARY KEY (id),
    CONSTRAINT fk_impressora_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE embalagem (
    id UUID NOT NULL,
    nome_emb VARCHAR(150) NOT NULL,
    custo_uni_emb NUMERIC(10, 2) NOT NULL,
    usuario_id UUID,
    CONSTRAINT embalagem_pkey PRIMARY KEY (id),
    CONSTRAINT fk_embalagem_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE configuracao (
    id UUID NOT NULL,
    markup_conf NUMERIC(10, 2) NOT NULL,
    markup_fixo_conf BOOLEAN NOT NULL DEFAULT FALSE,
    qnt_fil_peca NUMERIC(10, 2) NOT NULL,
    emb_fixa_conf BOOLEAN NOT NULL,
    custo_emb_fixa_conf NUMERIC(10, 2),
    formulario_avancado_conf BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_id UUID,
    CONSTRAINT configuracao_pkey PRIMARY KEY (id),
    CONSTRAINT fk_configuracao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE calculo_historico (
    id UUID NOT NULL,
    nome_peca_hist VARCHAR(150) NOT NULL,
    foto_peca_hist TEXT,
    tipo_foto_peca_hist VARCHAR(30),
    nome_fil_hist VARCHAR(150) NOT NULL,
    preco_rolo_hist NUMERIC(10, 2) NOT NULL,
    peso_rolo_fil NUMERIC(10, 2) NOT NULL,
    qntd_fil_hist NUMERIC(10, 2) NOT NULL,
    nome_imp_hist VARCHAR(150) NOT NULL,
    pot_hist NUMERIC(10, 2) NOT NULL,
    temp_hist BIGINT NOT NULL,
    tarifa_hist NUMERIC(10, 2) NOT NULL,
    custo_fil_hist NUMERIC(10, 2) NOT NULL,
    custo_energia_hist NUMERIC(10, 2) NOT NULL,
    nome_emb_hist VARCHAR(150) NOT NULL,
    custo_emb_hist NUMERIC(10, 2) NOT NULL,
    custo_total_hist NUMERIC(10, 2) NOT NULL,
    markup_hist NUMERIC(10, 2) NOT NULL,
    preco_sug_hist NUMERIC(10, 2) NOT NULL,
    data_hist TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    plataforma_venda_hist VARCHAR(30),
    tipo_anuncio_hist VARCHAR(30),
    percentual_comissao_hist NUMERIC(10, 2),
    custo_adicional_plataforma_hist NUMERIC(10, 2),
    percentual_imposto_hist NUMERIC(10, 2),
    frete_vendedor_hist NUMERIC(10, 2),
    taxa_cpf_shopee_hist BOOLEAN,
    frete_gratis_tik_tok_hist BOOLEAN,
    percentual_afiliado_hist NUMERIC(10, 2),
    preco_venda_plataforma_hist NUMERIC(10, 2),
    taxas_plataforma_hist NUMERIC(10, 2),
    lucro_liquido_hist NUMERIC(10, 2),
    tipo_pessoa_shopee_hist VARCHAR(10),
    mais450_pedidos_shopee_hist BOOLEAN,
    isencao_comissao_tik_tok_hist BOOLEAN,
    faixa_peso_pacote_hist VARCHAR(30),
    formulario_avancado_hist BOOLEAN,
    id_filamento_fk UUID,
    id_impressora_fk UUID,
    id_embalagem_fk UUID,
    id_peca_fk UUID,
    usuario_id UUID,
    CONSTRAINT calculo_historico_pkey PRIMARY KEY (id),
    CONSTRAINT fk_calculo_historico_filamento FOREIGN KEY (id_filamento_fk) REFERENCES filamento (id),
    CONSTRAINT fk_calculo_historico_impressora FOREIGN KEY (id_impressora_fk) REFERENCES impressora (id),
    CONSTRAINT fk_calculo_historico_embalagem FOREIGN KEY (id_embalagem_fk) REFERENCES embalagem (id),
    CONSTRAINT fk_calculo_historico_peca FOREIGN KEY (id_peca_fk) REFERENCES peca (id),
    CONSTRAINT fk_calculo_historico_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE item_calculo_historico (
    id UUID NOT NULL,
    nome_filamento_item VARCHAR(150) NOT NULL,
    cor_filamento_item VARCHAR(80),
    codigo_cor_item VARCHAR(7),
    preco_rolo_item NUMERIC(10, 2) NOT NULL,
    peso_rolo_item NUMERIC(10, 2) NOT NULL,
    quantidade_item NUMERIC(10, 2) NOT NULL,
    custo_filamento_item NUMERIC(10, 2) NOT NULL,
    id_calculo_fk UUID NOT NULL,
    id_filamento_fk UUID,
    CONSTRAINT item_calculo_historico_pkey PRIMARY KEY (id),
    CONSTRAINT fk_item_calculo_historico_calculo FOREIGN KEY (id_calculo_fk)
        REFERENCES calculo_historico (id),
    CONSTRAINT fk_item_calculo_historico_filamento FOREIGN KEY (id_filamento_fk)
        REFERENCES filamento (id)
);

CREATE INDEX idx_peca_usuario ON peca (usuario_id);
CREATE INDEX idx_filamento_usuario ON filamento (usuario_id);
CREATE INDEX idx_impressora_usuario ON impressora (usuario_id);
CREATE INDEX idx_embalagem_usuario ON embalagem (usuario_id);
CREATE INDEX idx_configuracao_usuario ON configuracao (usuario_id);
CREATE INDEX idx_calculo_historico_usuario ON calculo_historico (usuario_id);
