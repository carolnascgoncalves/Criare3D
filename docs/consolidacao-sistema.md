# Consolidação do sistema — 14/11/2026

## Entregue

- [x] Orçamentos oferecem preservar valores do cálculo ou recalcular com preços atuais. A proposta guarda uma fotografia financeira própria; alterar cadastros não muda silenciosamente seus custos. Quantidade, consumo e tempo continuam sendo calculados para o pedido.
- [x] Embalagem opcional para o recálculo quando o cálculo original usava um valor fixo e essa configuração foi desativada.
- [x] Confirmação única de vendas de impressão 3D e outras vendas. Duas requisições simultâneas da mesma confirmação produzem um único registro; mudar os parâmetros exige nova confirmação. Controle transacional e separado por empresa.
- [x] Cadastro de clientes compradores com nome, telefone, e-mail, observações, edição e arquivamento. Clientes podem ser selecionados nos orçamentos; seus contatos são copiados, e o pedido permanece vinculado ao cadastro por ID.
- [x] Extrato com filtros por período, descrição e tipo, paginação e origem. Registra compras, ajustes, produção, devolução, perdas, vendas e cancelamentos na mesma transação da operação.
- [x] Busca de catálogo no servidor com páginas de 20 resultados para peças, filamentos, impressoras, embalagens, produtos e compradores. O rascunho conserva seleções existentes e cadastros feitos durante o cálculo.
- [x] Salvamento automático dos orçamentos usa validação silenciosa. Envios comuns mostram progresso; falhas nas vendas preservam os valores para correção.
- [x] CSS da navegação separado em `navegacao.css`, componentes novos compartilhados e dez regras exatamente duplicadas removidas. As regras existentes não foram eliminadas indiscriminadamente para evitar regressões.
- [x] Controller de orçamento formatado em blocos legíveis. Dados de origem dos custos preparados para exibição, com sinalização de propostas antigas sem fotografia completa.
- [x] Dashboard e detalhes usam o mesmo arredondamento de embalagem compartilhada em cancelamentos parciais.
- [x] Testes unitários, de templates, integração com PostgreSQL, concorrência e testes de navegador adicionados ao CI.

## Compatibilidade

As alterações previamente preparadas no Git foram preservadas. Não houve push nem deploy.

- Migrações V46–V48 criam confirmações, compradores, extrato e vínculo do cliente. São aplicadas pelo Flyway na inicialização.
- O saldo de abertura do extrato registra a posição na atualização; não inventa compras ou movimentações anteriores. Históricos antigos continuam disponíveis nas telas de origem.
- Vendas antigas mantêm sua regra original de embalagem. Novas vendas podem distribuir uma ou mais embalagens entre vários produtos.
- Orçamentos antigos vinculados a um cálculo recuperam os valores desse cálculo. Propostas antigas criadas diretamente de rascunhos, sem fotografia completa, são sinalizadas para conferência: não é possível reconstruir valores que nunca foram armazenados.
- Arquivar um comprador não apaga nem altera os contatos de propostas já criadas.
- Contas administradas em `/clientes` são distintas dos clientes compradores em `/compradores`; o menu administrativo diz “Empresas e acessos”.

## Verificação executada

- 197 testes Java aprovados; um teste ignorado.
- 63 testes JavaScript aprovados e verificação de sintaxe dos scripts.
- Quatro testes de integração reais em PostgreSQL descartável: fluxo até cancelamento, embalagem compartilhada/isolamento/busca, confirmações simultâneas e arredondamento da dashboard.
- Todas as 48 migrações aplicadas desde um banco vazio; schema validado pelo Hibernate.
- Testes visuais de navegador adicionados à suíte Playwright/CI, sem execução visual local nesta rodada.

## Conferência rápida no navegador

1. Reiniciar a aplicação e cadastrar um cliente em **Comercial → Clientes**.
2. Criar orçamento, usar **Buscar no catálogo** no cliente e conferir telefone/e-mail.
3. Adicionar um cálculo usando cada modo de preços. No modo atual, escolher embalagem se necessário. Alterar quantidade e conferir o resultado.
4. Registrar produção, venda e cancelamento; conferir o extrato e o saldo.
5. Em outras vendas, vender 10 produtos com uma embalagem de R$ 1. Conferir custo total de embalagem de R$ 1.
6. Testar busca e seleção em outra página do catálogo e voltar pelo navegador, preservando o rascunho.
7. Conferir as telas novas em desktop e celular; os testes de navegador cobrem clientes, preenchimento de contatos e ausência de avisos durante preenchimento parcial.

## Teste de integração opcional

Usar somente um banco PostgreSQL descartável. Definir `CRIARE_TEST_DB_URL`, `CRIARE_TEST_DB_USER` e `CRIARE_TEST_DB_PASSWORD`, e executar:

```sh
mvn -Dcriare.integration=true -Dtest=ConsolidacaoIntegrationTest test
```

O CI executa esse comando contra o seu serviço PostgreSQL de teste antes da navegação com Playwright.
