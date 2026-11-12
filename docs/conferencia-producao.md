# Conferência da instalação

Estas configurações não foram aplicadas ao Render, GitHub ou S3 por este trabalho. Devem ser conferidas na instalação antes da comercialização.

## Origem do acesso no Render

1. Entre como ADMIN e abra `/clientes/diagnostico-origem` pelo domínio público HTTPS.
2. Guarde o campo `origem`. Repita pelo mesmo navegador e confirme que o hash é estável.
3. Repita pela rede móvel, com Wi-Fi desligado. O hash deve ser diferente. O log correspondente começa com `Diagnóstico de origem; origem=`; não registra o IP em claro.
4. Confirme também que a rota é negada para USER e deslogados.
5. Se redes diferentes apresentarem o mesmo hash, confira a cadeia de proxies antes de usar o limitador em produção. Tanto login quanto recuperação usam `getRemoteAddr()` após o processamento do Tomcat. Não resolva isso escolhendo cegamente o primeiro endereço enviado em `X-Forwarded-For`.

O padrão continua sendo `server.forward-headers-strategy=native`. A lista de proxies confiáveis deve corresponder à instalação; quando necessário, configure `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` e `SERVER_TOMCAT_REMOTEIP_TRUSTED_PROXIES` com expressões que identifiquem somente os proxies reais. Não use `.*`. A rota de diagnóstico não muda essa lista automaticamente.

Referências: [Render e IP do cliente](https://render.com/articles/how-render-handles-ddos-attacks), [RemoteIpValve do Tomcat](https://tomcat.apache.org/tomcat-11.0-doc/config/valve.html#Remote_IP_Valve).

## Backup, retenção e aviso de falha

- Configure `BACKUP_HABILITADO=true` nas variáveis do repositório. O workflow só estará ativado depois dessa configuração; o código local não confirma o valor remoto.
- Configure os segredos de banco/S3 descritos em `seguranca-operacao.md`, mais `BACKUP_ALERTA_WEBHOOK` e `BACKUP_HEARTBEAT_URL`, ambos HTTPS. O primeiro recebe POST JSON de falha. O segundo é um endereço de confirmação de um monitor independente, chamado por GET somente após restauração validada e upload concluído.
- Configure esse monitor para avisar se não receber uma confirmação diária, com tolerância para atrasos do agendamento, por exemplo 36 horas. Isso permite detectar também workflow que deixou de ser executado, situação em que um passo `if: failure()` não pode avisar.
- Ative as notificações de falha do GitHub como segundo canal. Execute o workflow manualmente, confira a cópia no bucket e a confirmação do monitor. Ensaie uma falha em ambiente de teste e confira a entrega do alerta; ter uma URL configurada não comprova recebimento.

O backup mantém uma transação de leitura aberta, exporta seu snapshot e usa esse snapshot no `pg_dump`. O arquivo `.rows` contém as contagens desse mesmo retrato em doze tabelas: clientes, usuários, peças, filamentos, impressoras, embalagens, histórico, itens do cálculo, vendas, pedidos, itens dos pedidos e fotos dos pedidos. A restauração compara essas contagens; qualquer divergência interrompe o fluxo antes do upload. Valores zero são válidos e também são comparados. Dump e manifesto recebem SHA-256. Isso verifica integridade e quantidade de registros, sem equivaler a uma comparação de todos os valores de cada linha.

No bucket, habilite versionamento e os quatro bloqueios de acesso público. O modelo `infra/backup-lifecycle.json` usa o prefixo `criare3d/`: o `BACKUP_S3_URI` deve usar esse mesmo prefixo, ou o modelo deve ser adaptado antes de aplicar. O workflow de cópia não altera políticas do bucket nem apaga backups antigos por conta própria.

**Política operacional proposta:** versões atuais expiram após 30 dias; versões antigas são removidas depois de um dia na condição de não atuais. A versão atual expirada vira uma versão antiga em buckets versionados. Portanto, essa configuração não representa uma promessa de remoção física exata no 30º dia; o processamento do provedor também é assíncrono. Marcadores expirados e uploads incompletos recebem limpeza própria no modelo.

Um cliente excluído permanece restaurável no sistema por 30 dias. Depois do expurgo, pode continuar em backups anteriores até essas cópias expirarem. Com o expurgo desligado, não existe uma data automática de remoção dos dados ativos. Esses prazos precisam ser definidos nos termos com revisão jurídica; esta documentação descreve o funcionamento técnico, sem declarar conformidade legal.

Ao recuperar um banco antigo em produção, mantenha a aplicação sem tráfego até reaplicar exclusões e suspensões ocorridas depois daquele backup. Para isso, preserve um registro operacional dessas solicitações fora do próprio backup restaurado. Essa reaplicação ainda não é automatizada pelo projeto.

Referências: [snapshot exportado do PostgreSQL](https://www.postgresql.org/docs/17/functions-admin.html#FUNCTIONS-SNAPSHOT-SYNCHRONIZATION), [expiração de objetos e versões no S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/lifecycle-expire-general-considerations.html), [eventos de agendamento do GitHub](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#schedule).

## Tarifas sem recompilar

Os valores anteriores foram preservados em `src/main/resources/tarifas-plataformas.properties`; nenhuma atualização de preços dos marketplaces foi presumida. Para substituir valores na instalação, configure `CRIARE_TARIFAS_ARQUIVO` com o caminho de outro arquivo `.properties`. Ele pode conter somente as chaves que mudaram. Reinicie a aplicação após alterar o arquivo.

São configuráveis comissões, valores fixos, limites e a matriz de frete. As fórmulas e os tipos de faixa permanecem no código. A inicialização rejeita chaves desconhecidas, números inválidos, listas com quantidade incorreta e faixas inconsistentes. Use ponto nos decimais e vírgula apenas para separar os elementos das listas.

Depois de conferir as tabelas com as plataformas, configure `CRIARE_TARIFAS_CONFERIDAS_EM` em `AAAA-MM-DD`. A data não é preenchida automaticamente por mudar o arquivo. Os históricos existentes preservam seus valores; alterar a configuração afeta novos cálculos. Essa configuração é global da instalação, não um editor de tarifas por cliente.

## Estrutura e interface

As dependências das classes de serviço extraídas são obrigatórias e recebidas por construtor. `VendaService` não usa mais repositório opcional nem relógio alternativo. A ausência de um bean impede a inicialização; os testes fornecem seus relógios e repositórios explicitamente.

A venda continua exigindo produção registrada. A tela informa a regra e a resposta `PRODUCAO_NECESSARIA` continua exibindo uma mensagem clara, sem movimentar filamento automaticamente.

A limpeza de CSS removeu blocos inteiramente repetidos dentro das mesmas condições de aplicação. O tamanho restante dos arquivos não foi tratado como motivo para apagar regras diferentes. A divisão completa do controller, do serviço de clientes e das fórmulas por plataforma continua sendo trabalho de organização separado, com testes e conferência visual próprios.


## Recuperação local do ADMIN

Se a senha atual foi esquecida, confira `ADMIN_EMAIL` e `ADMIN_PASSWORD` na configuração de execução do IntelliJ. Adicione temporariamente `ADMIN_REDEFINIR_SENHA=true` e reinicie a aplicação. A opção só redefine uma conta já identificada como ADMIN pelo mesmo e-mail; não transforma USER em ADMIN. Remova a variável depois da recuperação. Sem essa opção, a senha existente é preservada.

Os bloqueios de login ficam em memória e são limpos ao reiniciar a aplicação. Não é preciso alterar o banco para limpar a espera durante testes locais. O aviso de bloqueio agora aparece na página de login, com contagem regressiva; reenviar durante a espera não aumenta o bloqueio. Falhas internas do serviço de autenticação mostram um aviso de indisponibilidade, sem serem contadas como senha incorreta.
