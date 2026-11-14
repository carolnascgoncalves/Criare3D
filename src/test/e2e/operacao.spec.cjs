const { test, expect } = require('@playwright/test');
const id = (base,width) => `00000000-0000-4000-8000-${String(base+Number(width)).padStart(12,'0')}`;
async function semOverflow(page) {
 await expect.poll(() => page.evaluate(() => {
  const excesso=document.documentElement.scrollWidth-innerWidth;
  if(excesso<=0)return {excesso:0,elementos:[]};
  return {excesso:Math.ceil(excesso),elementos:[...document.querySelectorAll('main *')].filter(e=>{const r=e.getBoundingClientRect();return r.width>0&&(r.right>innerWidth+1||r.left<0);}).slice(0,12).map(e=>({tag:e.tagName,classe:e.className,direita:Math.round(e.getBoundingClientRect().right),largura:innerWidth}))};
 }),{message:'Elementos da página que ultrapassam a largura da tela'}).toEqual({excesso:0,elementos:[]});
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
 async function confirmar(botao) {
  const envio=await botao.evaluate(b=>({url:b.hasAttribute('formaction')?b.formAction:b.form.action,metodo:(b.hasAttribute('formmethod')?b.formMethod:b.form.method).toUpperCase()}));
  await botao.click();
  const dialogo=page.locator('#confirmacaoGlobal[open]');
  await expect(dialogo).toBeVisible();
  // O clique no diálogo apenas autoriza o Turbo; aguarde o POST antes de navegar.
  const [resposta]=await Promise.all([
   page.waitForResponse(r=>r.url()===envio.url&&r.request().method()===envio.metodo),
   dialogo.locator('[data-confirmacao-aceitar]').click()
  ]);
  expect(resposta.status(),`Falha ao salvar ${envio.url}`).toBeLessThan(400);
  expect(await resposta.finished()).toBeNull();
 }
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
 const envioPlanejamento=page.waitForResponse(r=>new URL(r.url()).pathname==='/simulacao/planejamentos'&&r.request().method()==='POST',{timeout:5000});
 await page.getByRole('button',{name:'Salvar na fila da impressora'}).click();
 const retornoPlanejamento=await envioPlanejamento.catch(async()=>{
  const avisos=await page.locator('.alerta-erro:visible,[data-sim="erro"]:visible').allTextContents();
  throw new Error('O botão não enviou o POST do planejamento. Avisos: '+avisos.join(' | '));
 });
 const destinoPlanejamento=retornoPlanejamento.headers().location||'';
 if(!/\/simulacao\/planejamentos(?:[?#]|$)/.test(destinoPlanejamento)){
  await page.locator('.alerta-erro:visible').first().waitFor({state:'visible',timeout:2000}).catch(()=>{});
  const avisos=await page.locator('.alerta-erro:visible,[data-sim="erro"]:visible').allTextContents();
  const parametros=new URLSearchParams(retornoPlanejamento.request().postData()||'');
  const enviados=Object.fromEntries(['calculoId','unidades','capacidade','inicio','intervalo','folga'].map(k=>[k,parametros.get(k)]));
  throw new Error('Planejamento recusado: HTTP '+retornoPlanejamento.status()+', destino '+destinoPlanejamento+', avisos '+avisos.join(' | ')+', campos '+JSON.stringify(enviados));
 }
 await expect(page).toHaveURL(/simulacao\/planejamentos/);
 await expect(page.locator('main')).toContainText('Peça navegador');await expect(page.locator('.planejamento-card')).toContainText('Produzido · aguardando venda');await semOverflow(page);
 await page.goto(`/vendas-estoque/lote/${lote}`);await page.locator('#vendaUnidades').fill('2');await confirmar(page.getByRole('button',{name:'Registrar venda',exact:true}));
 await expect(page.getByRole('heading',{name:'Lote vendido',exact:true})).toBeVisible();
 await expect(page.getByText('Todas as unidades foram vendidas',{exact:true})).toBeVisible();
 await expect(page.getByText('3 unidades vendidas · Nenhuma unidade restante em estoque.',{exact:true})).toBeVisible();
 await expect(page.locator('#vendaUnidades')).toHaveCount(0);
 await expect(page.getByRole('button',{name:'Registrar venda',exact:true})).toHaveCount(0);
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


test('login carrega os arquivos públicos sem redirecionar nem gravar destino protegido', async ({page}) => {
 const arquivos = ['/vendor/bootstrap-icons/bootstrap-icons.min.css', '/vendor/turbo.es2017-umd.js'];
 for (const caminho of arquivos) {
  const resposta = await page.request.get(caminho, {maxRedirects: 0});
  expect(resposta.status()).toBe(200);
  expect(resposta.headers()['content-type']).not.toContain('text/html');
 }
 await page.goto('/login');
 await expect(page.locator('.login-mostrar-senha .bi-eye')).toBeVisible();
 await page.locator('#password').fill('exemplo');
 await page.locator('.login-mostrar-senha').click();
 await expect(page.locator('#password')).toHaveAttribute('type', 'text');
});
