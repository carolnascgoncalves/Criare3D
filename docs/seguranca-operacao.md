# Segurança, operação e arquitetura

Veja também [a conferência da instalação](conferencia-producao.md) para diagnóstico de IP, retenção efetiva, monitoramento do backup e configuração externa de tarifas.

## Serviços

| Serviço | Responsabilidade |
|---|---|
| `CalculoService` | Carrega entradas e orquestra o cálculo, sem salvar histórico nem criar configuração. |
| `MotorCalculo` | Calcula com dados fornecidos, sem repositórios, sessão ou acesso à rede. |
| `CalculoValidador` | Valida entradas e limites do lote; máximo de 100 itens de filamento. |
| `MontadorHistorico` | Monta os snapshots de custos e materiais. |
| `HistoricoCalculoService` | Salva, consulta, exporta e exclui históricos sem operações vinculadas; conta usos dos cadastros. |
| `FotoCalculoService` | Fotos definitivas e miniaturas. |
| `FotoRascunhoService` | Uma foto temporária de até 3 MiB por usuário; sessão guarda somente UUID. Leitura exige usuário e tenant. |
| `ProducaoService` | Produção, devolução e saldo de filamento. |
| `VendaService` | Vendas e cancelamentos parciais, sem produzir automaticamente. |
| `AcaoEmLoteService` | Seleções com ordem de bloqueio estável e rollback integral em caso de falha. |
| `IndicadoresService` | Totais operacionais e financeiros. |
| `SimulacaoService` | Dados do histórico para a simulação. |
| `ResultadoCalculoPresenter` | Modelo de apresentação, comparação de plataformas e resumo do lote. |

Os controllers usam serviços concretos conforme sua necessidade. A interface e a implementação antigas do histórico foram removidas. Os resultados de produção e venda são `ResultadoOperacao`; códigos textuais são usados apenas nas mensagens e URLs existentes. As regras financeiras continuam em `BigDecimal`, `CalculoFinanceiro` e `CalculadoraProducao`.

O `Clock` da aplicação usa `America/Sao_Paulo` e pode ser substituído nos testes. Fotos temporárias expiram após 30 minutos e são removidas diariamente; salvar ou iniciar outro rascunho também remove a referência anterior. O formulário continua temporário na sessão, com timeout de 30 minutos de inatividade. Uma nova foto do mesmo usuário substitui a anterior, inclusive entre abas; um rascunho que perdeu a foto pede novo anexo ao salvar.

## Login e senhas

Depois de cinco falhas, conta e IP passam a ter espera independente de 30 segundos, que dobra por falha até uma hora. O sucesso limpa o contador da conta, mas preserva o da origem. O registro de falhas contém identificadores SHA-256, sem e-mail ou senha em claro. Os caches são limitados e expiram. **O limite é por instância:** uma implantação com várias réplicas deve usar um armazenamento compartilhado ou impor o limite também no proxy.

O IP é obtido de `getRemoteAddr()`. O Tomcat processa cabeçalhos encaminhados apenas por proxies considerados internos. Na implantação, restrinja o acesso direto à aplicação e configure o proxy de borda para substituir cabeçalhos enviados pelo cliente. Ajuste `server.tomcat.remoteip.internal-proxies` aos proxies reais quando necessário.

Senhas novas têm entre 12 e 72 caracteres e no máximo 72 bytes UTF-8. O próprio `PasswordEncoder` impede ultrapassar os 72 bytes; tentativas de login com senha maior retornam credenciais inválidas. `/vendor/**` é público, inclusive durante troca obrigatória de senha. CSRF continua ativo.

## Recuperação de acesso

Configure no ambiente:

| Variável | Valor |
|---|---|
| `CRIARE_PUBLIC_URL` | URL pública HTTPS, sem query string, fragmento ou credenciais. |
| `CRIARE_EMAIL_API_KEY` | Chave de envio do Resend. |
| `CRIARE_EMAIL_REMETENTE` | Endereço de um domínio validado no provedor. |

O link tem 256 bits aleatórios, validade de 15 minutos e uso único. Só seu SHA-256 vai para o banco. Uma nova solicitação revoga links anteriores. A resposta não confirma a existência da conta. Solicitações têm limite por e-mail e IP e fila limitada; usuários suspensos não recebem recuperação. Após a alteração, as sessões existentes expiram e o usuário entra novamente.

Sem configuração, a tela informa como pedir senha temporária ao administrador. Não há envio de e-mails durante os testes. Antes de ativar, ensaie a entrega para uma conta controlada, o uso único do link, expiração e encerramento das outras sessões. Referência do adaptador: <https://resend.com/docs/api-reference/emails/send-email>.

## Clientes excluídos

Excluir suspende o acesso e registra a data, preservando todas as tabelas. O administrador pode restaurar pela listagem durante 30 dias. Ativar/editar uma conta excluída exige restauração; repetir a exclusão não reinicia o prazo.

O expurgo diário de dados expirados fica **desativado até a configuração operacional**. Depois de validar backup e restauração, defina `CRIARE_PURGA_CLIENTES_HABILITADA=true`. Sem essa variável, os dados continuam preservados, mas a restauração pelo sistema se encerra em 30 dias.

O expurgo descobre tabelas com `tenant_id` no schema e ordena suas chaves estrangeiras, em vez de manter uma lista manual. Toda exclusão filtra o tenant. Ciclos ou restrições não previstas causam rollback, preservando os dados e gerando registro de erro. O teste de integração acrescenta uma tabela nova, confere todas as tabelas do schema e verifica que o outro cliente continua intacto.

## Saúde e alertas

O Actuator é incluído na compilação normal. `/actuator/health` e seus grupos são públicos, sem detalhes; outros endpoints são negados. Configure a verificação de disponibilidade da hospedagem para essa URL e ensaie a resposta com o banco indisponível.

Para alertas de erros 500/503, configure `CRIARE_ALERTA_WEBHOOK` com um endpoint HTTPS que aceite JSON. O payload contém aplicação, status e classe da exceção; não contém usuário, parâmetros, URL ou mensagem técnica. Há deduplicação por cinco minutos, timeout de rede e fila limitada. O receptor deve encaminhar o evento ao canal operacional escolhido. Sem essa variável, os erros continuam nos logs. Não há integração específica com o SDK do Sentry.

## Backup automático e restauração

`.github/workflows/backup.yml` executa diariamente às 05:00 UTC e permite execução manual. Antes de habilitar, configure:

| Tipo | Nome |
|---|---|
| Variável | `BACKUP_HABILITADO=true` |
| Variável | `BACKUP_AWS_REGION` |
| Segredos | `BACKUP_PGHOST`, `BACKUP_PGPORT`, `BACKUP_PGDATABASE`, `BACKUP_PGUSER`, `BACKUP_PGPASSWORD` |
| Segredos | `BACKUP_AWS_ACCESS_KEY_ID`, `BACKUP_AWS_SECRET_ACCESS_KEY`, `BACKUP_S3_URI` |
| Segredos | `BACKUP_ALERTA_WEBHOOK`, `BACKUP_HEARTBEAT_URL` |

Use um usuário com permissão de leitura de todas as tabelas do banco, SSL e um bucket privado dedicado. A rotina usa ferramentas PostgreSQL 17: ajuste a imagem se o servidor tiver versão principal mais nova. O formato custom é verificado com `pg_restore --list`; o dump e o manifesto de contagens recebem checksum SHA-256. As contagens de doze tabelas principais usam o mesmo snapshot do dump e são comparadas após a restauração. Cada execução restaura em PostgreSQL descartável antes de enviar a cópia ao S3 com criptografia AES256 no servidor. Dumps não são artifacts do GitHub nem entram no Git.

Configure no bucket versionamento e política de retenção (por exemplo, 30 cópias diárias), bloqueio de acesso público e permissões limitadas ao prefixo de backup. Habilite avisos de falha do workflow. O ensaio automatizado valida a restauração do arquivo; periodicamente inicie também uma aplicação isolada usando a cópia restaurada e confira os fluxos principais.

Scripts disponíveis para outro agendador: `scripts/backup-postgres.sh` e `scripts/testar-restauracao.sh`. Eles usam as variáveis padrão `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` e `PGSSLMODE`. O ensaio exige outro banco, com nome terminado em `_restore_test`, recusa banco existente e descarta somente o banco criado por ele. O job de navegador do CI também ensaia o backup do schema migrado e seus dados de teste.

## Migrations e verificação

A fonte de verdade é `src/main/resources/db/migration`. Nunca edite uma migration aplicada. A V34 acrescenta recuperação de senha, retenção de cliente e fotos de rascunho. Hibernate continua com `ddl-auto=validate`.

`baseline-on-migrate` agora é falso por padrão. Somente para um banco legado ainda sem histórico Flyway, após conferir seu schema e fazer backup, pode-se habilitar `FLYWAY_BASELINE_ON_MIGRATE=true` durante a incorporação inicial. Bancos já migrados não precisam disso.

Verificação completa, com Docker e acesso às dependências:

```powershell
.\mvnw.cmd clean verify
node --test src/test/js/*.test.cjs
```

Existe a propriedade `-DsemActuator` exclusivamente para testes locais sem acesso ao download dessa dependência. Ela não deve ser usada para gerar a distribuição de produção. Os testes de rotas verificam a cadeia real de segurança sem banco; migrations, isolamento, recuperação, fotos temporárias, rollback em lote e concorrência usam PostgreSQL real pelo Testcontainers.
