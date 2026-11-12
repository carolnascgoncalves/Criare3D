const { test, expect } = require('@playwright/test');
const id = (base,width) => `00000000-0000-4000-8000-${String(base+Number(width)).padStart(12,'0')}`;
async function semOverflow(page) {
 await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
}
test('cadastros, validação, produção, venda parcial, perdas e planejamento',async ({page},info) => {
 const largura=info.project.name,lote=id(500000,largura),peca=id(100000,largura);
 const erros=[];page.on('pageerror',erro=>erros.push(erro.message));
 await page.goto('/login');await page.locator('[name="email"]').fill(`e2e${largura}@example.test`);
 await page.locator('[name="password"]').fill('E2eSenha123!');await page.getByRole('button',{name:'Entrar',exact:true}).click();
 await expect(page).not.toHaveURL(/login/);
 // Um único aviso e destaque temporário. Modelo continua abrível/editável após erro.
 await page.goto('/impressora/novo');await page.locator('button[type="submit"]').last().click();
 await expect(page.locator('#nome')).toHaveClass(/campo-erro-temporario/);
 await expect(page.locator('#modelo')).toHaveClass(/campo-erro-temporario/);
 await expect(page.locator('.notificacao-flutuante.alerta-erro')).toHaveCount(1);
 await expect(page.locator('[data-campos-pendentes]')).toBeVisible();
 await page.locator('#modelo').fill('Modelo teste');await page.locator('#modelo').blur();await semOverflow(page);
 // O seletor continua funcionando depois de uma tentativa sem peça.
 await page.goto('/historico/novo');await page.locator('#abrirPecaSeletor').click();
 await expect(page.locator('[data-peca-lista]')).toContainText('Peça navegador');
 await page.locator('[data-peca-lista] button').first().click();
 await expect(page.locator('[data-reutilizar-peca]')).toBeVisible();await semOverflow(page);
 await page.goto(`/vendas-estoque/lote/${lote}`);
 const produzir=page.getByRole('button',{name:'Registrar produção do lote',exact:true});
 // O app usa diálogo próprio; confirmações são atendidas pelo botão do diálogo.
 async function confirmar(botao) { await botao.click(); const dialogo=page.locator('#confirmacaoGlobal[open]'); await expect(dialogo).toBeVisible(); await dialogo.locator('[data-confirmacao-aceitar]').click(); }
 await confirmar(produzir);await expect(page.getByText('3 unidades prontas disponíveis')).toBeVisible();
 await page.locator('#vendaUnidades').fill('2');await confirmar(page.getByRole('button',{name:'Registrar venda',exact:true}));
 await expect(page.getByText('1 unidades prontas disponíveis')).toBeVisible();
 await page.locator('.operacao-cancelar > summary').click();
 await page.locator('input[name="unidades"][id^="cancelar-"]').fill('1');await confirmar(page.locator('.operacao-cancelar form button'));
 await expect(page.getByText('2 unidades prontas disponíveis')).toBeVisible();
 await page.getByText('Perda de filamento',{exact:true}).click();await page.locator('#falhaFilamento').selectOption({label:'PLA navegador · Preto'});
 await page.locator('#falhaGramas').fill('5');await confirmar(page.getByRole('button',{name:'Registrar perda',exact:true}));
 await expect(page.getByText(/Material perdido: R\$ 0,50/)).toBeVisible();await semOverflow(page);
 await page.locator('#custoDescricao').fill('Acabamento');await page.locator('#custoValor').fill('12.50');await page.getByRole('button',{name:'Adicionar custo'}).click();await expect(page.locator('main')).toContainText('Acabamento');
 await page.goto('/simulacao');await page.locator('[data-sim="abrir-seletor"]').click();await page.locator('[data-sim="lista-calculos"] button').first().click();
 await expect(page.locator('#simUnidades')).toHaveValue('3');
 await page.getByRole('button',{name:'Salvar na fila da impressora'}).click();await expect(page).toHaveURL(/simulacao\/planejamentos/);
 await expect(page.locator('main')).toContainText('Peça navegador');await semOverflow(page);
 await page.goto(`/vendas-estoque/lote/${lote}`);await page.locator('#vendaUnidades').fill('2');await confirmar(page.getByRole('button',{name:'Registrar venda',exact:true}));
 await page.goto('/simulacao/planejamentos');await expect(page.locator('.planejamento-card')).toHaveCount(0);
 await page.getByRole('link',{name:'Concluídos',exact:true}).click();await expect(page.locator('.planejamento-card')).toContainText('Peça navegador');
 // A navegação Turbo conserva o menu durante rolagem na simulação.
 await page.goto('/simulacao');const antes=await page.locator('.menu-principal').boundingBox();await page.evaluate(()=>scrollTo(0,document.body.scrollHeight));
 const depois=await page.locator('.menu-principal').boundingBox();expect(Math.abs(antes.y-depois.y)).toBeLessThan(2);
 await page.goto('/peca/listar');await page.locator('[data-selecao-toggle]').click();await page.locator(`input.checkbox-selecao-listagem[value="${peca}"]`).check();await confirmar(page.getByRole('button',{name:'Arquivar selecionados',exact:true}));
 await page.goto('/catalogo/arquivados');await expect(page.locator('main')).toContainText('Peça navegador');
 await page.getByRole('button',{name:'Restaurar',exact:true}).click();await expect(page.locator('main')).not.toContainText('Peça navegador');
 expect(erros).toEqual([]);
});
