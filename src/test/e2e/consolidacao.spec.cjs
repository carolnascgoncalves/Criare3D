const {test,expect}=require('@playwright/test');
test('cliente cadastrado preenche os contatos e cálculo rápido não avisa durante preenchimento',async({page},info)=>{
 const largura=info.project.name,nome='Cliente navegação '+largura;
 const erros=[];page.on('pageerror',e=>erros.push(e.message));
 await page.goto('/login');await page.locator('[name=email]').fill(`e2e${largura}@example.test`);await page.locator('[name=password]').fill('E2eSenha123!');await page.getByRole('button',{name:'Entrar',exact:true}).click();
 await page.goto('/compradores');await page.getByRole('button',{name:'Novo cliente'}).first().click();await page.locator('#nome').fill(nome);await page.locator('#telefone').fill('11999991234');await page.locator('#email').fill('cliente@example.com');await page.getByRole('button',{name:'Salvar cliente',exact:true}).click();
 await expect(page.getByText(nome,{exact:true})).toBeVisible();
 await page.getByRole('searchbox',{name:'Buscar clientes'}).fill('Busca inexistente');await page.getByRole('searchbox',{name:'Buscar clientes'}).press('Enter');await page.locator('[data-clientes-limpar]').click();await expect(page.getByText(nome,{exact:true})).toBeVisible();
 await page.goto('/orcamentos/novo');await page.locator('.comprador-escolha .catalogo-buscar').click();const dialogo=page.getByRole('dialog',{name:'Buscar no catálogo'});await dialogo.getByRole('searchbox').fill(nome);await dialogo.getByRole('button',{name:nome,exact:true}).click();
 await expect(page.locator('[name=cliente]')).toHaveValue(nome);await expect(page.locator('[name=contato]')).toHaveValue('11999991234');await expect(page.locator('[name=email]')).toHaveValue('cliente@example.com');
 await expect(page.locator('[data-cliente-selecionado]')).toHaveText(nome);await page.locator('[data-comprador-limpar]').click();await expect(page.locator('[data-comprador-orcamento]')).toHaveValue('');await expect(page.locator('[name=cliente]')).toHaveValue(nome);
 await page.goto('/calculo-rapido');await page.locator('#rapidoNome').fill('Preenchimento parcial');await page.waitForTimeout(800);await expect(page.locator('.notificacao-flutuante.alerta-erro')).toHaveCount(0);
 await expect.poll(()=>page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);expect(erros).toEqual([]);
});
