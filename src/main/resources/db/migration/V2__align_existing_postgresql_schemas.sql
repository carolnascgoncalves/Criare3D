-- Os dois bancos atuais foram conferidos: não há filamentos sem cor.
-- A nulabilidade passa a refletir as entidades atuais do projeto.
ALTER TABLE filamento ALTER COLUMN cor_fil SET NOT NULL;
ALTER TABLE filamento ALTER COLUMN marca_fil DROP NOT NULL;

-- Índices por usuário presentes no Neon e adicionados ao banco local.
CREATE INDEX IF NOT EXISTS idx_peca_usuario ON peca (usuario_id);
CREATE INDEX IF NOT EXISTS idx_filamento_usuario ON filamento (usuario_id);
CREATE INDEX IF NOT EXISTS idx_impressora_usuario ON impressora (usuario_id);
CREATE INDEX IF NOT EXISTS idx_embalagem_usuario ON embalagem (usuario_id);
CREATE INDEX IF NOT EXISTS idx_configuracao_usuario ON configuracao (usuario_id);
CREATE INDEX IF NOT EXISTS idx_calculo_historico_usuario ON calculo_historico (usuario_id);

-- No banco local havia uma segunda FK idêntica para peca(id).
ALTER TABLE calculo_historico DROP CONSTRAINT IF EXISTS fk_calculo_peca;
