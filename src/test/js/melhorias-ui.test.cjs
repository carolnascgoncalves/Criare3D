const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const script = name => fs.readFileSync(path.join(__dirname, '../../main/resources/static/js', name), 'utf8');
class Node {
  constructor(){this.children=[];this.style={};this.className='';this.textContent='';this.classList={add(){}};}
  appendChild(n){this.children.push(n);return n;}
  append(...nodes){nodes.forEach(n=>this.appendChild(n));}
  replaceChildren(...nodes){this.children=nodes.flatMap(n=>n.fragment?n.children:[n]);}
  setAttribute(){}
}
function simulacao() {
 const document={readyState:'loading',addEventListener(){},createElement:()=>new Node(),createDocumentFragment:()=>Object.assign(new Node(),{fragment:true})};
 const context={document,window:{},Date,Intl,Math,Number};
 vm.createContext(context);
 const source=script('simulacaoProducao.js').replace(/\}\)\(\);\s*$/, 'globalThis.api = {planejar, desenharLinha, quadro};})();');
 vm.runInContext(source,context);
 return context.api;
}
function plano(api,inicio,minutos){return api.planejar({inicio,unidades:1,porImpressao:1,tempoPorUnidadeMinutos:minutos,aquecimentoMinutos:0,intervalo:0,folga:0,filamentos:[]});}
for (const [hora,minutos,classes] of [[10,120,['sim-periodo-dia']],[21,120,['sim-periodo-dia','sim-noite']],[5,120,['sim-noite','sim-periodo-dia']],[23,1500,['sim-noite','sim-periodo-dia','sim-noite']]]) {
 test(`faixas dia/noite: início ${hora}h e duração ${minutos}min`,()=>{
  const api=simulacao(),p=plano(api,new Date(2026,10,11,hora),minutos),ui={linha:new Node(),eixo:new Node()};
  api.desenharLinha(ui,p);
  const periodos=ui.linha.children.filter(n=>['sim-noite','sim-periodo-dia'].includes(n.className));
  assert.deepEqual(Array.from(periodos,n=>n.className),classes);
  assert.ok(Math.abs(periodos.reduce((s,n)=>s+parseFloat(n.style.width),0)-100)<1e-6);
 });
}
test('tempo decorrido aumenta e termina no total sem confundir a hora do dia',()=>{
 const api=simulacao(),p=plano(api,new Date(2026,10,11,10),120),ui={linha:new Node(),eixo:new Node()};
 for(const k of ['relogioData','relogioHora','relogioDuracao','impressaoAtual','pecasProntas','camada','corte','portal','bico'])ui[k]=new Node();
 const vis=api.desenharLinha(ui,p);
 api.quadro(ui,p,vis,0.5,null);assert.equal(ui.relogioDuracao.textContent,'1h 00min de 2h 00min');assert.equal(ui.relogioHora.textContent,'11:00');
 api.quadro(ui,p,vis,1,null);assert.equal(ui.relogioDuracao.textContent,'2h 00min de 2h 00min');assert.equal(ui.pecasProntas.textContent,'1 de 1');
});
function campo(value=''){return {value,defaultValue:value,dataset:{},listeners:{},addEventListener(n,f){this.listeners[n]=f;},focus(){}};}
function orcamento() {
 const fields={};for(const id of ['nomeCliente','prazoOrcamento','pagamentoOrcamento','validadeOrcamento','freteOrcamento','mensagemOrcamento'])fields['#'+id]=campo(id==='freteOrcamento'?'0':id==='nomeCliente'?'Ana':id==='mensagemOrcamento'?'':'A combinar');
 const root={dataset:{peca:'Dragão',unidades:'2',precoUnitario:'10',precoTotal:'20',precoBaseTotal:'20',orcamentoInicializado:'true'}};
 fields['.pagina-orcamento']=root;fields['[data-restaurar-mensagem]']=campo();
 const events={};const document={readyState:'loading',querySelector:s=>fields[s]||null,addEventListener:(n,f)=>events[n]=f};
 vm.runInNewContext(script('orcamento.js'),{document,window:{},Intl,Number,WeakSet});events['DOMContentLoaded']();
 return {fields,events};
}
test('restaurar orçamento limpa personalização e preserva cliente e frete',()=>{
 const {fields:f}=orcamento();
 f['#freteOrcamento'].value='7';f['#freteOrcamento'].listeners.input();
 f['#prazoOrcamento'].value='Texto alterado';f['#mensagemOrcamento'].value='Mensagem livre';f['#mensagemOrcamento'].listeners.input();
 f['[data-restaurar-mensagem]'].listeners.click();
 assert.equal(f['#prazoOrcamento'].value,'A combinar');
 assert.match(f['#mensagemOrcamento'].value,/Olá, Ana/);
 assert.match(f['#mensagemOrcamento'].value,/Frete:.*7,00/);
 assert.match(f['#mensagemOrcamento'].value,/Total:.*27,00/);
 assert.doesNotMatch(f['#mensagemOrcamento'].value,/Mensagem livre|Texto alterado/);
 assert.equal(f['#mensagemOrcamento'].dataset.editadaManualmente,undefined);
});
test('mensagens ficam dentro da tela de 320px ao lado do menu lateral',()=>{
 const notices=[{style:{},dataset:{},classList:{contains:()=>false},getBoundingClientRect:()=>({height:60})},{style:{},dataset:{},classList:{contains:()=>false},getBoundingClientRect:()=>({height:60})}];
 const events={};const document={querySelector:()=>({getBoundingClientRect:()=>({height:900,right:64})}),querySelectorAll:s=>s==='.notificacao-flutuante'?notices:[],addEventListener:(n,f)=>events[n]=f};
 vm.runInNewContext(script('notificacoes.js'),{document,window:{innerWidth:320,setTimeout(){},addEventListener(){}}});
 events['DOMContentLoaded']();
 assert.equal(notices[0].style.top,'12px');assert.equal(notices[0].style.left,'76px');assert.equal(notices[0].style.width,'232px');
 assert.equal(notices[1].style.top,'80px');
});

test('preencher filamentos sugere rolos mas exige confirmação e limpa ao trocar peça',async()=>{
 class Elemento extends Node {
  constructor(){super();this.dataset={};this.listeners={};this.value='';this.hidden=false;this.classList={add(){},remove(){}};}
  addEventListener(n,f){(this.listeners[n]??=[]).push(f);}
  async disparar(n,event={}){for(const f of this.listeners[n]||[])await f(event);}
  setCustomValidity(){} checkValidity(){return true;} focus(){} matches(){return false;}
 }
 class Select extends Elemento {
  constructor(options){super();this.options=options;}
  get selectedOptions(){return [this.options.find(o=>o.value===this.value)||this.options[0]];}
 }
 const options=[{value:'',textContent:'Escolha',dataset:{}},{value:'rolo',textContent:'PLA Preto',dataset:{codigoCor:'#000000',estoque:'100'}}];
 const items=[];
 const novoItem=()=>{
  const e=new Elemento(),select=new Select(options),q=new Elemento();e.map={'[data-campo-filamento]':select,'[data-campo-quantidade]':q,'.filamento-calculo-cabecalho strong':new Elemento(),'[data-remover-filamento]':new Elemento()};e.querySelector=s=>e.map[s]||null;e.remove=()=>items.splice(items.indexOf(e),1);return e;
 };
 const lista=new Elemento();lista.querySelectorAll=()=>items;lista.querySelector=()=>items[0]||null;lista.appendChild=e=>{items.push(e);return e;};lista.replaceChildren=()=>items.splice(0);
 const peca=new Select([{value:'a',textContent:'Peça A',dataset:{gramas:'10',temArquivo:'true'}},{value:'b',textContent:'Peça B',dataset:{gramas:'20',temArquivo:'false'}}]);peca.value='a';
 const ids={listaFilamentos:lista,modeloFilamento:{content:{firstElementChild:{cloneNode:novoItem}}},pecaId:peca};
 for(const id of ['adicionarFilamento','secaoFilamentos','resumoConsumoPeca','previaMateriaisPeca','nomePecaResumo','consumoPrevistoResumo','listaResumoConsumo','totalFilamentoResumo','totalPrevistoResumo','statusConsumoResumo','formCalculo'])ids[id]=new Elemento();
 const aplicar=new Elemento(),previa=new Elemento(),status=new Elemento();
 const document={getElementById:id=>ids[id]||null,querySelector:s=>({'[data-aplicar-materiais]':aplicar,'[data-previa-materiais-lista]':previa,'[data-previa-materiais-status]':status}[s]||null),createElement:()=>new Elemento(),addEventListener(){}};
 const context={document,window:{confirm:()=>true,salvarRascunhoCalculo(){},mostrarNotificacao(){}},AbortController,fetch:async()=>({ok:true,json:async()=>[{material:'PLA',corHex:'#000000',pesoGramas:10}]}),Event,Number,Math,Set};
 vm.createContext(context);vm.runInContext(script('filamentosCalculo.js'),context);vm.runInContext('inicializarFilamentosCalculo()',context);
 await new Promise(resolve=>setImmediate(resolve));
 await aplicar.disparar('click');
 assert.equal(items.length,1);assert.equal(items[0].dataset.confirmado,'false');assert.equal(items[0].hidden,false);assert.equal(items[0].map['[data-campo-filamento]'].value,'rolo');
 const item=items[0],confirmar={closest:()=>item};
 await lista.disparar('click',{target:{closest:s=>s==='[data-confirmar-filamento]'?confirmar:null}});
 assert.equal(item.dataset.confirmado,'true');assert.equal(item.dataset.confirmadoPeloUsuario,'true');assert.equal(item.hidden,true);
 peca.value='b';await peca.disparar('change');
 assert.equal(items[0].dataset.confirmado,'false');assert.equal(items[0].map['[data-campo-filamento]'].value,'');
 peca.value='a';await peca.disparar('change');
 assert.equal(items[0].dataset.confirmado,'false');assert.equal(items[0].hidden,false);
});
