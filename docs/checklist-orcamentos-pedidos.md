# Conferência de orçamentos e pedidos

## Como o sistema ficou organizado

- **Cadastros:** peça, material, impressora e embalagem continuam sendo cadastrados uma vez.
- **Calcular peça:** ferramenta rápida para conferir custos. O resultado pode entrar em um orçamento sem precisar salvar um cálculo intermediário.
- **Cálculos salvos:** referências reutilizáveis de material, impressora e parâmetros. Continuam atendendo produção e venda avulsas.
- **Orçamentos e pedidos:** um único registro acompanha proposta, aprovação, reserva, produção, pagamento e entrega.
- **Simulação:** continua disponível para estudos avulsos. A etapa Produção do orçamento usa a jornada de trabalho e a mesma fila de impressoras.
- **Fila de produção:** reúne os planejamentos avulsos e os trabalhos de pedidos aprovados. Os trabalhos de pedidos têm acesso direto ao pedido.
- **Vendas e estoque:** mostra o saldo livre para venda, descontando as reservas dos pedidos.

O orçamento tem quatro etapas: **Peças → Produção → Preço → Proposta**. Até a aprovação, ele não reserva peças nem consome filamento. Aprovar reserva o estoque escolhido e cria apenas os lotes que faltam. Confirmar produção consome material. Confirmar entrega registra as vendas. Registrar pagamento é uma operação separada.

## Preparação

- [ ] Inicie a aplicação e confira a execução das migrations V35, V36 e V37, sem modificar migrations anteriores nem usar `repair`.
- [ ] Use uma conta de teste com peça, filamento, impressora e embalagem cadastrados e ao menos um cálculo salvo.
- [ ] Anote o saldo do filamento e a quantidade de unidades prontas antes dos testes.
- [ ] Faça a conferência no computador e com a janela em aproximadamente 320 px de largura.

## Montagem e proposta

- [ ] Entre em **Orçamentos e pedidos → Novo orçamento**, busque uma referência por nome/material/impressora e adicione uma peça.
- [ ] Adicione uma segunda referência. Altere quantidades, remova um item e confira o resumo.
- [ ] Use **Calcular outra peça** dentro do rascunho; no resultado escolha **Adicionar ao orçamento em edição**. Confira que voltou ao mesmo orçamento.
- [ ] Comece um cálculo avulso novo e use **Usar em um orçamento**. Confira que foi criado outro rascunho.
- [ ] Em um lote com 3 unidades livres, solicite 5 e escolha usar 2 prontas. Confira **2 do estoque e 3 a produzir**.
- [ ] Salve, saia e reabra o rascunho. Quantidades, preços, cliente e horários devem permanecer salvos.
- [ ] Configure expediente e dias de trabalho; teste uma impressão que termina depois do expediente e uma troca que cai no fim de semana. A próxima impressão deve começar no próximo horário permitido.
- [ ] Confira uma referência de **29 min/unidade**, quantidade **5**, capacidade **3**: são 145 min imprimindo, mais aquecimento em duas impressões, intervalo entre elas, folga e eventuais esperas pelo expediente/fila.
- [ ] Ajuste preço por unidade, desconto e os dois campos de frete. O total é peças − desconto + frete cobrado; o custo do envio reduz o lucro.
- [ ] Tente um desconto maior que o subtotal ou um preço inválido. Deve aparecer um aviso de erro; os campos não devem perder os dados válidos.
- [ ] Informe o cliente e marque a proposta como enviada. O saldo físico e o filamento devem continuar iguais aos valores anteriores.
- [ ] Abra **Proposta / salvar PDF**, confira foto, nome, material, quantidades, preço, desconto, frete, total e observações. Custos internos e lucro não devem aparecer.
- [ ] Teste **Copiar mensagem** e a prévia de impressão/PDF. Peças sem foto devem conservar o ícone.

## Aprovação, estoque e fila

- [ ] Aprove o pedido misto. Só as unidades faltantes devem ganhar novo lote e planejamento; as prontas devem ficar reservadas.
- [ ] Em Vendas e estoque, tente vender unidades já reservadas ou desfazer a produção desse lote. O sistema deve bloquear e orientar a gerenciar pelo pedido.
- [ ] Venda avulsamente somente a unidade livre do lote. Essa venda deve continuar funcionando.
- [ ] Monte duas propostas usando o mesmo saldo. Depois de aprovar uma, a outra deve reconferir o estoque e recusar uma reserva acima do disponível.
- [ ] Abra o mesmo pedido em duas abas e aprove nas duas. A segunda ação deve pedir atualização, sem criar outro lote ou outra reserva.
- [ ] Confira a fila da impressora: pedido aprovado deve aparecer com **Acompanhar pedido**; não deve permitir retirar isoladamente esse trabalho pela fila.
- [ ] Aprove um pedido atendido inteiramente pelo estoque. Ele deve ficar pronto para entregar, sem gerar nova produção.

## Produção, pagamento e entrega

- [ ] Deixe passar o horário previsto: o pedido não deve registrar produção ou venda sozinho.
- [ ] Confirme a produção do item pendente. O material deve ser descontado uma única vez e o planejamento deve sair da fila ativa.
- [ ] Tente produzir com filamento insuficiente. Nenhuma baixa parcial de material deve ser salva.
- [ ] Registre o pagamento integral antes da entrega. Isso não deve registrar vendas nem consumir material.
- [ ] Tente entregar um pedido com produção pendente. A entrega deve ser recusada.
- [ ] Entregue o pedido pronto. As vendas devem ter o vínculo do pedido e as reservas devem ser liberadas.
- [ ] Confira o histórico de vendas e o dashboard. Não deve haver venda duplicada ao repetir o envio da mesma tela.

## Cancelamento e regressão

- [ ] Cancele uma proposta sem aprovação: nenhum saldo deve mudar.
- [ ] Cancele um pedido aprovado sem produção: reservas devem ser liberadas e trabalhos retirados da fila.
- [ ] Cancele um pedido já produzido: as peças continuam prontas no estoque e o filamento não é devolvido automaticamente.
- [ ] Cancele um pedido entregue que dividia um lote com uma venda avulsa. Só as vendas daquele pedido devem ser canceladas; a venda avulsa deve permanecer ativa.
- [ ] Se havia pagamento, o registro deve permanecer e a tela deve lembrar de combinar o reembolso. O sistema não movimenta dinheiro em banco ou gateway.
- [ ] Confirme que calcular, salvar histórico, produzir, vender parcialmente e cancelar vendas avulsas continuam funcionando.
- [ ] Com outra conta/cliente, tente abrir um orçamento, foto ou lote do primeiro cliente. Os dados não devem aparecer.

## Limites desta primeira versão

Até 20 itens por orçamento e 500 unidades por item. Entrega e registro de pagamento são integrais; pagamentos parciais, entregas fracionadas e cobrança automática não estão incluídos. A previsão é estimada e considera inícios/trocas no expediente; a impressora pode terminar um trabalho fora dele. Cancelar ou concluir trabalhos não reorganiza automaticamente os horários dos outros pedidos.

## Validação técnica

- Validação local: `mvn verify` com o perfil local `-DsemActuator`, excluindo os testes que dependem de Docker. Foram aprovados **132 testes Java** e **35 testes JavaScript**.
- Os testes PostgreSQL/Testcontainers e de navegador foram adicionados ao conjunto do projeto, mas precisam rodar no CI ou em uma máquina com Docker disponível. O ambiente de edição não permitiu executar o Docker nem confirmar visualmente as telas.
- Antes de publicar, executar `mvn verify` sem excluir testes e `npm run test:e2e` no ambiente descartável configurado no CI. Conferir o ensaio de backup/restauração, que agora compara também pedidos, itens e fotos dos pedidos.

## Ajustes de seleção, arquivo e simulação

- [ ] Selecionar uma referência sem produção e tentar usar uma unidade do estoque: o campo deve permanecer em zero e a requisição deve ser recusada no servidor se adulterada.
- [ ] Selecionar um lote com mais de uma unidade e com foto: deve adicionar normalmente, mantendo a foto do item.
- [ ] Ao escolher a peça, o seletor deve recolher; a seta permite abrir novamente, sem perder quantidades.
- [ ] Na etapa Preço, valores devem mostrar duas casas decimais.
- [ ] Em Produção, alterar jornada e clicar Simular produção; pular a animação e conferir peças prontas e previsão. Alterar os horários deve esconder a animação antiga até simular novamente.
- [ ] Arquivar um rascunho pela listagem, abrir Arquivados e restaurar. Pedidos em andamento não devem poder ser escondidos; propostas arquivadas precisam ser restauradas antes de aprovar.
- [ ] Informar telefone com DDD e abrir WhatsApp. Informar e-mail e abrir o aplicativo de e-mail. Conferir a mensagem antes de enviar. Contato inválido não deve gerar um link externo.

## Frete e fluxo revisado

- [ ] Novo orçamento deve manter a seleção aberta até adicionar a peça. Na etapa 01, o bloco de adicionar outra peça deve começar recolhido, inclusive quantidades e botão de adicionar.
- [ ] Na etapa 02, clicar Simular produção; conferir mudança de cor entre itens, datas e faixas. Pular deve liberar Salvar e continuar; alterar horários deve exigir nova simulação.
- [ ] Na proposta, salvar rascunho sem nome de cliente ainda deve ser permitido; enviar continua exigindo o nome. Valores de produção/preço não devem ser alterados pelo salvamento dos contatos.
- [ ] Seguir [a configuração do Melhor Envio](melhor-envio.md), conectar no Sandbox e consultar um frete pelo botão Calcular com frete na etapa 03.
- [ ] Conferir peso em kg e dimensões em cm; selecionar a cotação deve preencher o custo e o valor cobrado, sem compra de etiqueta.
- [ ] Confirmar as migrations V37 e V38 antes dos testes de arquivo e integração. A autorização real e a cotação ainda precisam de credenciais válidas e teste no Sandbox.

## Linha do tempo e entrada direta

- [ ] Novo orçamento abre diretamente na etapa 01, sem uma seleção anterior ao fluxo de etapas. Antes da primeira peça, sair não deve deixar rascunho vazio salvo.
- [ ] Adicionar a primeira peça deve manter a etapa 01 e recolher o bloco de seleção para adicionar outra.
- [ ] A prévia deve ter uma única barra com fundo de dia/noite e impressões de cores diferentes. Trabalhos simultâneos, se houver, usam trilhas internas no mesmo eixo de tempo.
- [ ] Repetir e pular são ícones no cabeçalho; ambos devem funcionar pelo teclado e mostrar descrição ao passar o mouse.
- [ ] Antes de Salvar e continuar, conferir começo, conclusão, impressão, esperas, folga e duração total de cada peça. Não deve aparecer a antiga barra azul de progresso.

## Seleção múltipla, desconto e acompanhamento

- [ ] Marcar duas peças, informar quantidades distintas e adicionar de uma vez. Campos de peças desmarcadas não devem ser enviados e cada item deve respeitar seu próprio saldo de estoque.
- [ ] Usar desconto 15 em um subtotal de R$ 200: desconto de R$ 15. Usar 15%: desconto de R$ 30. Frete não entra na base percentual. Valores inválidos, negativos ou maiores que o subtotal devem ser recusados.
- [ ] Alterar a quantidade após informar porcentagem: o percentual deve acompanhar o novo subtotal.
- [ ] Simular, voltar para as peças e adicionar outra: a prévia antiga não deve reaparecer como válida no retorno por cache. Simular novamente e conferir todas as peças e os períodos de espera.
- [ ] Abrir a mensagem pelo aplicativo de e-mail e pelo Gmail no navegador, usando um e-mail válido no contato.
- [ ] Após marcar a proposta como enviada, conferir o cartão do cliente e a fila com horários separados.
- [ ] Filtrar todos os status da listagem, inclusive em Arquivados, e conferir a paginação.
- [ ] Reiniciar a aplicação para aplicar V39, que mantém a opção percentual do desconto.


### Materiais e peças prontas
- [ ] Monte um orçamento misto: 2 unidades a produzir e 4 de outro tipo vindas do estoque. Na prévia, confira os dois nomes e suas quantidades; somente a produção ocupa a barra de tempo.
- [ ] Use o mesmo filamento em dois itens e confira se o consumo é somado, incluindo a purga de todas as impressões.
- [ ] Nas etapas Peças, Produção e Proposta, confira consumo, saldo atual e falta de cada filamento.
- [ ] Com 1.120 g faltantes e rolos de 1.000 g a R$ 80, a reposição estimada deve ser 2 rolos / R$ 160, sem alterar o preço ao cliente.
- [ ] Abra o pedido após confirmar produção: os materiais já descontados não devem aparecer novamente como consumo pendente.
- [ ] Confira que a proposta/PDF do cliente não mostra os saldos e custos internos de reposição.


### Reabrir pedidos e acompanhar aprovação
- [ ] Abra um pedido já aprovado com produção pendente: materiais devem carregar sem erro.
- [ ] Em outra proposta enviada, marque Cliente aprovou e confira a página de retorno. Saia e reabra o pedido pela lista.
- [ ] O bloco Próxima etapa deve explicar a ação atual. Entregar só aparece quando todas as peças estão prontas; cancelar fica em Outras ações.
- [ ] Sem falta de filamento, os detalhes de consumo começam recolhidos. Com falta, começam abertos e indicam reposição.
- [ ] Confira os blocos no celular: ações, materiais e fila não devem ultrapassar a largura da tela.
