ALTER TABLE configuracao
    ADD COLUMN mao_obra_tempo_fixo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN tempo_mao_obra_fixo_minutos BIGINT NOT NULL DEFAULT 0;
