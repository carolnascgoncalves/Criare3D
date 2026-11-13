# Validação das otimizações

## Implementado nesta rodada

- Dashboard: montagem extraída do controller para DashboardDadosService; cache Caffeine de 30 segundos, até 500 entradas, por tenant, período e dia local. Armazena dados de apresentação e DTOs, sem entidades Hibernate. Todas as consultas do painel são reaproveitadas, inclusive ranking e primeira venda.
- Resumo SQL: seleção por UNION de históricos calculados e produzidos no período. Vendas continuam calculadas independentemente pela data de venda, inclusive quando o cálculo é antigo.
- V40: índices por tenant e chave de peça, filamento e impressora produzida, além de filamento dos itens. Não altera migrations anteriores.
- Batch fetch e escrita Hibernate: 50; ordenação de inserts/updates. Mantido open-in-view=false.
- Frete manual: exceção específica em vez de procurar palavras na mensagem.
- Metadados 3MF: erros Jackson registrados sem incluir o JSON nos logs; causa preservada na preparação.
- Cadastros de peça, filamento, impressora e embalagem: estado de envio do Turbo.

## Checklist funcional

- [ ] CI build-and-test passa, incluindo aplicação da V40 em PostgreSQL e cálculos de dashboard por data de venda/produção.
- [ ] CI browser passa nas larguras 320 e 1280.
- [ ] Dashboard abre nos períodos 30, 90 e Tudo, inclusive em conta vazia.
- [ ] Trocar de conta não mostra totais da conta anterior.
- [ ] Registrar venda/produção e reabrir o dashboard após 30 segundos mostra os novos totais.
- [ ] Cadastro válido mostra Salvando durante o envio; cadastro inválido continua editável.
- [ ] Mercado Livre acima da faixa estimada solicita frete manual; demais erros mantêm a mensagem correta.
- [ ] Importar 3MF e reutilizar materiais continua funcionando.

## Medição no Render

- Conferir região do serviço e do banco antes de atribuir lentidão às consultas.
- JAVA_OPTS já é aceito no Dockerfile. Não foi imposto um coletor nem um limite de heap sem medir a memória do plano.
- Para diagnóstico temporário, configurar HIBERNATE_SLOW_QUERY_MS=100; remover após medir. Não habilitar log dos parâmetros SQL.
- Comparar primeira abertura e reaberturas do mesmo período, com a mesma base e condições. Sem benchmark executado em produção, não há percentual de ganho comprovado.
- EXPLAIN (ANALYZE, BUFFERS) deve ser feito em ambiente controlado com volume representativo. Um Seq Scan isoladamente não comprova falta de índice.

## Não implementado nesta rodada

- Separar dados 3MF da entidade Peca ou substituir leituras por projeções específicas.
- Resultados/rascunhos independentes por aba e persistência além da sessão.
- Separar calculadoras por plataforma, fotos/sessão e demais controllers.
- Reorganização ampla do CSS.
- Dashboard assíncrono com skeleton: medir a primeira abertura após as otimizações antes de adicionar outro fluxo.
- Invalidação imediata do painel: nesta versão a consistência é de até 30 segundos; não há cache no fluxo operacional de estoque/vendas.

O checklist inicial da home e o suporte a JAVA_OPTS já existiam.
