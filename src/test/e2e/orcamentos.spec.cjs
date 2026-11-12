const {test,expect}=require('@playwright/test');
test('orçamento vira pedido sem movimentar estoque antes da aprovação',async({page},info)=>{
 const largura=Number(info.project.name);
 const lote=`00000000-0000-4000-8000-${String(500000+largura).padStart(12,'0')}`;
 const erros=[];page.on('pageerror',e=>erros.push(e.message));
 async function confirmar(nome) {
  await page.getByRole('button',{name:nome,exact:true}).click();
  const dialogo=page.locator('#confirmacaoGlobal[open]');await expect(dialogo).toBeVisible();
  await dialogo.locator('[data-confirmacao-aceitar]').click();
 }
 async function semOverflow(){await expect.poll(()=>page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);}
 await page.goto('/login');await page.locator('[name=email]').fill(`e2e${largura}@example.test`);
 await page.locator('[name=password]').fill('E2eSenha123!');await page.getByRole('button',{name:'Entrar',exact:true}).click();
 await expect(page).not.toHaveURL(/login/);
 await page.goto(`/orcamentos/novo?calculo=${lote}`);
 await page.locator(`article[data-pedido-multi]:has(input[value="${lote}"]) [data-pedido-marcar]`).check();
 await page.locator('article[data-pedido-multi]:has([data-pedido-marcar]:checked) [data-multi-quantidade]').fill('2');
 await page.getByRole('button',{name:'Adicionar peças selecionadas',exact:true}).click();
 await expect(page).toHaveURL(/orcamentos\/[a-f0-9-]+\?etapa=1/);
 await expect(page.locator('main')).toContainText('Nenhum estoque foi reservado');await semOverflow();
 await page.locator('[name=cliente]').fill('Cliente navegador');await page.locator('[name=cliente]').press('Tab');
 await page.getByRole('button',{name:'Continuar: prazo',exact:true}).click();
 await expect(page.locator('[data-sim-fim]')).not.toBeEmpty();
 await page.getByRole('button',{name:'Continuar: preço',exact:true}).click();
 await expect(page).toHaveURL(/etapa=3/);await semOverflow();
 await page.getByRole('button',{name:'Continuar: revisar e enviar',exact:true}).click();
 await expect(page).toHaveURL(/etapa=4/);

 await page.getByRole('button',{name:'Salvar e marcar como enviada',exact:true}).click();
 await expect(page.getByRole('button',{name:'Cliente aprovou',exact:true})).toBeVisible();
 const proposta=await page.getByRole('link',{name:'Proposta / salvar PDF'}).getAttribute('href');
 const preview=await page.context().newPage();await preview.goto(proposta);
 await expect(preview.locator('[data-pedido-proposta]')).toContainText('Cliente navegador');
 await expect(preview.locator('[data-pedido-proposta]')).not.toContainText('Lucro estimado');await preview.close();
 await confirmar('Cliente aprovou');
 await expect(page.getByRole('button',{name:'Confirmar produção concluída',exact:true})).toBeVisible();
 await confirmar('Confirmar produção concluída');
 await expect(page.locator('main')).toContainText('Pronto para entregar');
 await confirmar('Registrar pagamento integral');await expect(page.locator('main')).toContainText('Pagamento registrado');
 await confirmar('Confirmar entrega e venda');await expect(page.locator('main')).toContainText('Entregue');await semOverflow();
 await page.getByText('Outras ações',{exact:true}).click();await confirmar('Cancelar pedido');await expect(page.locator('main')).toContainText('Cancelado');
 await expect(page.locator('main')).toContainText('eventual reembolso');
 expect(erros).toEqual([]);
});
