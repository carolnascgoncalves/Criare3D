# Revisão de operação e experiência

## Fluxo principal

1. Calcular cria uma estimativa. Salvar preserva os custos e os parâmetros daquele momento.
2. Em **Vendas e estoque**, abrir a operação do lote mostra o consumo de filamento antes de produzir.
3. Registrar produção baixa material e grava a data da produção. Vender exige um lote já produzido.
4. A venda informa quantidade; preço, desconto e total de taxas/impostos/frete são opcionais e **por unidade**.
5. Cancelar uma quantidade devolve apenas essas unidades ao estoque de peças. A produção e seu consumo de filamento permanecem registrados.

## Alterações entregues

- Bloqueio transacional do lote para impedir vendas simultâneas acima do saldo; versão otimista do histórico e tratamento de conflitos.
- Cancelamentos preservam a quantidade original, a quantidade cancelada e a data do último cancelamento. Não apagam o registro da venda.
- Preço, desconto, taxas e resultado específicos de cada venda; comparação com o resultado previsto no cálculo original.
- Dashboard considera unidades não canceladas e valores registrados nas vendas. Os custos de produção continuam sendo estimativas.
- Data da produção separada da data do cálculo. Lotes antigos continuam sem data de produção, pois ela não pode ser reconstruída com segurança; ficam fora dos indicadores de produção por período.
- Registro de tentativas extras com falha: unidades, filamento, gramas, tempo e observação. Desconta o material perdido e permite corrigir o registro sem devolução duplicada.
- Perdas extras são mostradas separadamente da reserva estimada de falha. Não alteram automaticamente o custo original nem são descontadas novamente do resultado das vendas.
- Planejamentos persistidos a partir da simulação. A fila considera o último término planejado da mesma impressora e inclui aquecimento, intervalos e folga. Retirar um planejamento não muda os horários dos demais.
- Planejar não reserva filamento e não cria automaticamente produção nem um novo lote. A operação aberta pela fila corresponde ao lote de referência; quantidades ajustadas na simulação pertencem ao planejamento.
- Arquivamento e restauração de peças, filamentos, impressoras e embalagens. Saem das escolhas para novos cálculos; os históricos existentes são preservados.
- Reutilização de um cálculo específico ou do último cálculo da peça. Parâmetros são editáveis, filamentos devem ser conferidos e custos são recalculados. O resultado compara custo anterior e atual sem alterar o histórico original.
- Resumo recolhido de campos pendentes, além do aviso único no topo e do destaque temporário. Permite localizar os campos depois que o aviso desaparece.
- Orçamento para o comprador exibe foto e condições de prazo, pagamento e validade também na impressão. O PDF do resultado continua sendo o documento interno de custos.
- Primeiros passos existentes preservados, com orientação para registrar produção e venda após calcular.
- Operação de estoque/vendas e tratamento de fotos separados da fachada de cálculo. Removidos o aviso antigo de recálculo bloqueado e o caminho de exclusão que devolvia material de uma produção.
- Seletores compartilhados extraídos para `componentes.css`, sem copiar as regras. Novas telas usam os mesmos componentes responsivos.
- Histórico com operações vinculadas é preservado. Filamentos usados em registros de falha também são protegidos contra exclusão; arquivar continua disponível.

## Banco

As migrations **V30** e **V31** acrescentam os campos de operação, arquivamento, perdas e planejamentos. O Flyway as aplica na inicialização. Elas não preenchem datas ou preços reais fictícios nos registros antigos.

## Verificação

Executados no ambiente desta revisão:

- 52 testes Java sem depender de Docker, incluindo regras de venda, validação, mapeamento de cálculos antigos, renderização Thymeleaf das novas telas e análise das consultas HQL pelo Hibernate.
- 16 testes JavaScript.
- Verificação de sintaxe dos scripts e de espaços do diff.

Configurados para o GitHub Actions:

- Integração com PostgreSQL descartável: migrations, produção por data real, venda e cancelamento parcial, concorrência, correção de perdas, fila por impressora, isolamento e preservação dos cadastros.
- Navegador Chromium em **320 px e 1280 px**: validação, seletores, produção, vendas, perdas, planejamento, menu fixo, arquivamento e restauração.
- Relatórios, capturas em falhas e log da aplicação anexados ao job de navegador.

A execução local de integração e navegador ficou impedida pelo acesso ao Docker/conexões locais e pelo download de dependências de navegador. Esses testes estão configurados, mas o resultado do CI ainda precisa ser observado.

## Conferência manual

1. Calcular uma peça, salvar, abrir **Vendas e estoque** e revisar os materiais antes de produzir.
2. Produzir um lote de 3, vender 2 e cancelar 1 dessa venda: devem restar 2 unidades prontas, com 1 vendida e a produção preservada.
3. Informar preço, desconto e taxas reais; conferir resultado por unidade e comparação com o previsto.
4. Registrar uma perda de 5 g e corrigi-la: o saldo deve voltar exatamente ao valor anterior, mesmo se a correção for enviada novamente.
5. Salvar dois planejamentos para a mesma impressora: o segundo deve começar após o primeiro. Retirar o primeiro não deve deslocar o segundo.
6. Arquivar e restaurar um cadastro; conferir que o histórico permanece disponível e que o cadastro reaparece no cálculo após restaurar.
7. Reutilizar um cálculo antigo, alterar um custo de cadastro e conferir a comparação no resultado, preservando o registro antigo.
8. Conferir avisos/campos pendentes, impressão do orçamento e ausência de rolagem horizontal em 320 px e desktop.

## Ajustes de uso em 320 px e desktop

- Tempo informado no cálculo é por unidade: 5 unidades × 29 min = 145 min, mais aquecimento por impressão. Capacidade 3 gera duas impressões; a última tem 2 unidades e 58 min de impressão.
- O seletor de embalagem exibe nome e preço. A comparação com o cadastro também usa o tempo por unidade.
- Ações dos cartões de estoque e produção ficam em coluna. A operação usa indicadores e textos curtos.
- Custos personalizados aceitam descrição e valor; perda de filamento permanece opcional e os gramas são formatados com duas casas decimais. Os extras ficam separados do custo original.
- Planejamentos têm página e atalho próprios no menu. A venda de todas as unidades conclui a fila do lote; venda parcial não conclui. Os encerrados ficam na aba Concluídos e não reabrem automaticamente com um cancelamento de venda.
- Arquivamento fica disponível no modo de seleção das quatro listagens, com ícone de arquivo. A restauração usa cartões na página Arquivados.
- O script de retorno restaura filtros apenas nos links de voltar; não substitui o destino da operação do lote.
- A migration V32 acrescenta custos personalizados e o estado concluído do planejamento.

Verificação desta etapa: 60 testes Java e 20 testes JavaScript passaram localmente. Banco e navegador continuam cobertos pelos jobs configurados no CI; não houve conferência visual local.

## Histórico da V32

A V32 permanece com o checksum originalmente aplicado (`50938521`). O ajuste que separa planejamentos cancelados dos concluídos está na V33. O teste `MigrationHistoricoTest` protege esse checksum; novas alterações devem usar novas versões de migration.
