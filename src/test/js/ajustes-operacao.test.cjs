const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const script=nome=>fs.readFileSync(`src/main/resources/static/js/${nome}`,'utf8');
function calculo(aquecimento=0) {
 const nodes={};
 const campo=(value='')=>({value,dataset:{},hidden:false,textContent:'',classList:{add(){},remove(){}}});
 for(const nome of ['resumoImpressoes','tempoTotalLoteResumo','avisoImpressaoParcial','resumoTempoImpressao','statusTempoImpressao','tempoInformadoResumo','tempoPrevistoResumo'])nodes[nome]=campo();
 const tempoInput=campo('29'),unidadesInput=campo('5'),pecasPorImpressaoInput=campo('3');
 const pecaSelect={value:'peca',selectedOptions:[{dataset:{tempo:'29'}}]};
 const impressoraSelect={selectedOptions:[{dataset:{tempoAquecimento:String(aquecimento)}}]};
 const context={document:{getElementById:id=>nodes[id]},tempoInput,unidadesInput,pecasPorImpressaoInput,pecaSelect,impressoraSelect,Intl};
 vm.createContext(context);
 const src=script('calculoEtapas.js');
 const producao=src.slice(src.indexOf('    function numeroImpressoes()'),src.indexOf('    unidadesInput?.addEventListener'));
 const comparacao=src.slice(src.indexOf('    const resumoTempo ='),src.indexOf('    function atualizarAvisoAquecimento()'));
 vm.runInContext(producao+comparacao+';globalThis.atualizar=atualizarResumoProducao;',context);
 return {nodes,tempoInput,pecasPorImpressaoInput,atualizar:context.atualizar};
}
test('5 unidades de 29 min em grupos de 3 resultam em 145 min e última impressão de 58 min',()=>{
 const c=calculo();c.atualizar(true);
 assert.equal(c.tempoInput.value,'29');assert.match(c.nodes.tempoTotalLoteResumo.textContent,/145 min.*2 impressões/);
 assert.match(c.nodes.avisoImpressaoParcial.textContent,/2 peças.*58 min/);
 assert.equal(c.nodes.statusTempoImpressao.textContent,'Tempo dentro do esperado');
 c.pecasPorImpressaoInput.value='2';c.atualizar(true);assert.equal(c.tempoInput.value,'29');
 assert.match(c.nodes.tempoTotalLoteResumo.textContent,/145 min.*3 impressões/);
});
test('aquecimento é somado uma vez por impressão e não muda o tempo cadastrado por unidade',()=>{
 const c=calculo(4);c.atualizar(true);
 assert.equal(c.tempoInput.value,'29');assert.match(c.nodes.tempoTotalLoteResumo.textContent,/153 min.*4 min de aquecimento/);
 assert.match(c.nodes.avisoImpressaoParcial.textContent,/58 min/);
});
test('tempo manual por unidade é preservado ao ajustar a capacidade',()=>{
 const c=calculo();c.tempoInput.value='35';c.tempoInput.dataset.tempoManual='true';c.atualizar(true);
 assert.equal(c.tempoInput.value,'35');assert.match(c.nodes.tempoTotalLoteResumo.textContent,/175 min/);
 assert.match(c.nodes.avisoImpressaoParcial.textContent,/70 min/);
 assert.equal(c.nodes.statusTempoImpressao.textContent,'Tempo acima do esperado');
});
test('restaurar filtros só altera o link de voltar, preservando o destino da operação do lote',()=>{
 const origem={url:'/historico/listar?busca=teste',y:200};
 const operacao={href:'http://localhost/vendas-estoque/lote/123',hasAttribute:()=>false,matches:s=>s.includes('.detalhes-acoes a')};
 const voltar={href:'http://localhost/historico/listar',hasAttribute:()=>false,matches:()=>true};
 const source=script('restaurarRolagem.js');
 const fn=source.slice(source.indexOf('    function atualizarLinksVoltar()'),source.indexOf('    function restaurarRolagem()'));
 const context={URL,CHAVE_ORIGEM:'origem',lerEstado:()=>origem,ehDetalhes:()=>true,origemValidaDoHistorico:()=>true,sessionStorage:{removeItem(){}},window:{location:{origin:'http://localhost',pathname:'/historico/detalhes/123'}},document:{querySelectorAll:()=>[operacao,voltar]}};
 vm.runInNewContext(fn+';atualizarLinksVoltar();',context);
 assert.equal(operacao.href,'http://localhost/vendas-estoque/lote/123');assert.equal(voltar.href,origem.url);
});
