# Polimento dos módulos

## Navegação
- Uma entrada “Calcular preço” com escolha entre consulta provisória e cálculo salvo.
- “Meus cálculos” reúne os registros; “Planejar produção” abre agenda/fila e permite novo planejamento.
- Identidade do cliente preservada; login mantém a identidade cost3d.

## Fluxos ajustados
- Início: saudação curta, checklist que desaparece após configuração e primeiro cálculo, recentes antes dos alertas, atalho para valor da hora.
- Dashboard: quatro indicadores principais; métricas complementares em “Ver mais”; cores de plataformas distintas.
- Peças: imagens maiores, último preço calculado quando existir, ação Calcular e menu de ações com arquivamento e exclusão confirmados.
- Cadastros: áreas para soltar arquivos 3MF e barras de salvar; os campos importados e as miniaturas existentes continuam editáveis.
- Filamentos: filtro de saldo baixo dos itens da página, saldo total e quantidade equivalente de rolos; corrigir saldo no menu secundário.
- Cálculo rápido: preço atualizado pelo mesmo motor do servidor após preencher os campos obrigatórios, barra fixa e atalhos das configurações. Nada é salvo automaticamente.
- Orçamentos: números de exibição sequenciais, status com cores e próxima ação; UUIDs e links mantidos.
- Vendas e estoque: disponibilidade e reservas separadas, aviso Tudo reservado, lucro negativo destacado.
- Produção: agenda por impressora com início e fim reais dos planejamentos da página; modo lista disponível. O planejamento novo seleciona o cálculo mais recente, sem salvar nem registrar produção automaticamente.
- Configurações: índice, interruptores e exemplo de preço; depreciação da impressora tem link direto para a configuração que a ativa.
- Custos da loja: metas primeiro, novo formulário recolhido, rótulos específicos por modo e prévia do custo por peça.
- Outras vendas: produtos primeiro e formulários em janelas separadas; registrar venda por produto e embalagem opcional recolhida.
- Embalagens: quantidade de cálculos em que são usadas; consultas feitas em lote.

## Conferência manual
1. Com uma loja nova, cadastrar impressora, filamento e peça; concluir o primeiro cálculo e verificar o checklist.
2. Importar 3MF, escolher rolos, impressora e embalagem no cálculo rápido; mudar quantidade/markup e conferir preço e consumo. Limpar e voltar pelo navegador.
3. Em Peças/Filamentos, abrir ⋯, arquivar/restaurar e cancelar uma exclusão. Conferir filtros e paginação.
4. Criar dois orçamentos e conferir números diferentes e crescentes; abrir um registro antigo pelo mesmo link.
5. Reservar todo o saldo de um lote e verificar Tudo reservado; conferir lucro negativo e cancelamento de venda.
6. Conferir agenda/lista com duas impressoras, vários dias e paginação; criar uma simulação sem alterar estoque.
7. Configurar uma meta de R$ 1.500 e 300 peças: prévia de R$ 5 por peça; confirmar recuperação somente em vendas registradas.
8. Cadastrar outro produto, registrar venda com embalagem e cancelar unidades; conferir dashboard e categoria.
9. Repetir em celular e com teclado, incluindo abrir/fechar janelas com Escape.

## Dados e numeração
A migração V44 atribui números estáveis aos orçamentos existentes e usa uma sequência do banco para os novos. A sequência é global, permite lacunas em transações canceladas e mantém os UUIDs como identificadores internos. Nomes cadastrados de impressoras não são renomeados automaticamente.
