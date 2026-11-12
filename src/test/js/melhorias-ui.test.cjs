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
for (const [hora,minutos,classes] of [[10,120,['sim-periodo-dia']],[17,120,['sim-periodo-dia','sim-noite']],[18,60,['sim-noite']],[6,60,['sim-periodo-dia']],[5,120,['sim-noite','sim-periodo-dia']],[23,1500,['sim-noite','sim-periodo-dia','sim-noite']]]) {
 test(`faixas dia/noite: início ${hora}h e duração ${minutos}min`,()=>{
  const api=simulacao(),p=plano(api,new Date(2026,10,11,hora),minutos),ui={linha:new Node(),eixo:new Node()};
  api.desenharLinha(ui,p);
  const periodos=ui.linha.children.filter(n=>['sim-noite','sim-periodo-dia'].includes(n.className));
  assert.deepEqual(Array.from(periodos,n=>n.className),classes);
  assert.ok(Math.abs(periodos.reduce((s,n)=>s+parseFloat(n.style.width),0)-100)<1e-6);
 });
}
test('simulação curta distingue 2 min de impressão de 10 min de intervalo e mostra início e fim',()=>{
 const api=simulacao();
 const p=api.planejar({inicio:new Date(2026,10,11,21,12),unidades:2,porImpressao:1,tempoPorUnidadeMinutos:1,aquecimentoMinutos:0,intervalo:10,folga:10,filamentos:[]});
 assert.equal(p.minutosImpressao,2);assert.equal(p.minutosIntervalos,10);assert.equal(p.total,12);assert.equal(p.eixo,13);
 const ui={linha:new Node(),eixo:new Node()};ui.eixo.clientWidth=180;api.desenharLinha(ui,p);
 const pausa=ui.linha.children.find(n=>n.className==='sim-intervalo');
 assert.equal(pausa.textContent,'Intervalo: 10min');
 assert.deepEqual(Array.from(ui.eixo.children,n=>n.textContent),['21:12','21:25']);
 assert.equal(ui.linha.children.filter(n=>n.className==='sim-bloco').length,2);
});
test('uma impressão de 1 minuto sem pausa mantém blocos e eixo válidos',()=>{
 const api=simulacao(),p=plano(api,new Date(2026,10,11,21,12),1),ui={linha:new Node(),eixo:new Node()};
 api.desenharLinha(ui,p);
 assert.equal(p.minutosImpressao,1);assert.equal(p.minutosIntervalos,0);
 assert.deepEqual(Array.from(ui.eixo.children,n=>n.textContent),['21:12','21:13']);
 assert.equal(ui.linha.children.some(n=>n.className==='sim-intervalo'),false);
 assert.equal(ui.linha.children.find(n=>n.className==='sim-bloco').style.width,'100%');
});

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
 vm.runInNewContext(script('notificacoes.js'),{document,URLSearchParams,window:{location:{search:''},innerWidth:320,setTimeout(){},addEventListener(){}}});
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

test('formulário inválido emite um único aviso e destaca todos os campos por três segundos',async()=>{
 const eventos={},avisos=[],timers=[];
 class Campo {
  constructor(){this.type='text';this.disabled=false;this.willValidate=true;this.validity={valid:false,valueMissing:true};this.dataset={};this.attrs={};this.classes=new Set();this.classList={contains:c=>this.classes.has(c),add:c=>this.classes.add(c),remove:c=>this.classes.delete(c)};}
  closest(){return null;}setAttribute(k,v){this.attrs[k]=v;}removeAttribute(k){delete this.attrs[k];}focus(){this.focado=true;}
 }
 class Form {constructor(){this.method='post';this.elements=[new Campo(),new Campo()];}querySelector(){return null;}}
 const form=new Form();
 const document={readyState:'loading',querySelectorAll:s=>s==='form'?[form]:[],addEventListener:(n,f)=>eventos[n]=f};
 vm.runInNewContext(script('validacaoInline.js'),{document,window:{mostrarNotificacao:(...a)=>avisos.push(a)},HTMLFormElement:Form,WeakMap,Set,queueMicrotask,setTimeout:(fn,ms)=>{timers.push({fn,ms});return timers.length;},clearTimeout(){}});
 eventos.DOMContentLoaded();assert.equal(form.noValidate,true);
 let bloqueado=false;eventos.submit({target:form,preventDefault(){bloqueado=true;}});
 await Promise.resolve();assert.equal(bloqueado,true);assert.equal(avisos.length,1);assert.match(avisos[0][0],/Não foi possível prosseguir/);
 form.elements.forEach(c=>assert.equal(c.classes.has('campo-erro-temporario'),true));
 assert.ok(timers.every(t=>t.ms===3000));timers.forEach(t=>t.fn());
 form.elements.forEach(c=>assert.equal(c.classes.size,0));
});

test('erro de validação do servidor aparece no topo em vez de abaixo do campo',async()=>{
 const eventos={},avisos=[];const classes=new Set();
 const campo={type:'text',dataset:{},classList:{contains:()=>false,add:c=>classes.add(c),remove:c=>classes.delete(c)},closest:()=>null,setAttribute(){},removeAttribute(){}};
 const erro={textContent:'O nome da peça é obrigatório.',dataset:{},hasAttribute:()=>false,closest:()=>({querySelectorAll:()=>[campo]})};
 const document={readyState:'loading',querySelectorAll:s=>s==='.erro-campo'?[erro]:[],addEventListener:(n,f)=>eventos[n]=f};
 vm.runInNewContext(script('validacaoInline.js'),{document,window:{mostrarNotificacao:m=>avisos.push(m)},WeakMap,Set,queueMicrotask,setTimeout(){},clearTimeout(){}});
 eventos.DOMContentLoaded();await Promise.resolve();
 assert.equal(erro.hidden,true);assert.equal(classes.has('campo-erro-temporario'),true);assert.match(avisos[0],/nome da peça é obrigatório/);
});

test('seletor utiliza a miniatura salva e mantém ícone quando não há imagem',()=>{
 const eventos={};const root={querySelector:()=>({})};
 const document={readyState:'loading',querySelector:()=>root,getElementById:()=>({}),addEventListener:(n,f)=>eventos[n]=f,createElement:tag=>({tag,addEventListener(){}})};
 const context={document,window:{},WeakSet,Date};vm.createContext(context);
 vm.runInContext(script('pecaSeletor.js').replace('        const resumo = opcao =>','        globalThis.api = {imagem}; return;\n        const resumo = opcao =>'),context);eventos.DOMContentLoaded();
 const foto=context.api.imagem({dataset:{fotoUrl:'/peca/1/arquivo/placa/1'}});
 assert.equal(foto.tag,'img');assert.equal(foto.src,'/peca/1/arquivo/placa/1');
 assert.equal(context.api.imagem({dataset:{}}).className,'sim-seletor-sem-foto');
});

test('avisos de erro de chamadas separadas substituem o anterior',()=>{
 const avisos=[];
 const main={prepend:n=>avisos.unshift(n)};
 function criar(){const e={dataset:{},style:{},className:'',setAttribute(){},getBoundingClientRect:()=>({height:50}),remove(){const i=avisos.indexOf(e);if(i>=0)avisos.splice(i,1);}};e.classList={contains:c=>e.className.split(' ').includes(c),add(){}};return e;}
 const document={querySelector:s=>s==='main.pagina'?main:{getBoundingClientRect:()=>({right:64})},querySelectorAll:s=>s.includes('alerta-erro')?avisos.filter(n=>n.classList.contains('alerta-erro')):s==='.notificacao-flutuante'?[...avisos]:[],createElement:criar,addEventListener(){}};
 const window={innerWidth:320,location:{search:''},setTimeout(){},addEventListener(){}};
 vm.runInNewContext(script('notificacoes.js'),{document,window,URLSearchParams});
 window.mostrarNotificacao('Preencha o nome.');window.mostrarNotificacao('Preencha o modelo.');window.mostrarNotificacao('Preencha a potência.');
 assert.equal(avisos.length,1);assert.equal(avisos[0].textContent,'Preencha a potência.');
});

test('erro na peça não impede abrir e reabrir o seletor',async()=>{
 const eventos={},observadores=[];
 class E extends Node {
  constructor(){super();this.dataset={};this.attrs={};this.listeners={};this.classes=new Set();this.classList={add:c=>this.classes.add(c),remove:c=>this.classes.delete(c),contains:c=>this.classes.has(c)};this.hidden=false;this.disabled=false;this.value='';}
  addEventListener(n,f){(this.listeners[n]??=[]).push(f);}
  fire(n,e={}){(this.listeners[n]||[]).forEach(f=>f(e));}
  setAttribute(n,v){this.attrs[n]=v;for(const o of observadores)if(o.target===this&&o.options.attributeFilter.includes(n))o.fn();}
  removeAttribute(n){delete this.attrs[n];for(const o of observadores)if(o.target===this&&o.options.attributeFilter.includes(n))o.fn();}
  getAttribute(n){return this.attrs[n]??null;}
  get childElementCount(){return this.children.length;}
  focus(){this.focado=true;}
 }
 const select=new E();select.type='select-one';select.validity={valid:false,valueMissing:true};select.options=[{value:'',textContent:'Escolha',dataset:{}},{value:'a',textContent:'Peça A',dataset:{gramas:'10',tempo:'20'}}];select.selectedOptions=[select.options[0]];
 const root=new E(),abrir=new E(),painel=new E(),busca=new E(),lista=new E(),vazio=new E();painel.hidden=true;let icone=new E();icone.replaceWith=n=>{icone=n;icone.replaceWith=()=>{};};abrir.querySelector=()=>icone;
 const nodes={'[data-peca-abrir]':abrir,'[data-peca-painel]':painel,'[data-peca-busca]':busca,'[data-peca-lista]':lista,'[data-peca-vazio]':vazio,'[data-peca-nome]':new E(),'[data-peca-resumo]':new E(),'[data-peca-cadastrar]':new E()};
 root.querySelector=s=>nodes[s]||null;root.contains=()=>true;
 select.closest=s=>s==='.campo'?{querySelector:q=>q==='.sim-seletor-abrir'?abrir:null}:null;
 const document={readyState:'loading',querySelector:()=>root,getElementById:()=>select,querySelectorAll:()=>[],createElement:()=>new E(),createDocumentFragment:()=>Object.assign(new E(),{fragment:true}),addEventListener:(n,f)=>(eventos[n]??=[]).push(f)};
 class Observer {constructor(fn){this.fn=fn;observadores.push(this);}observe(target,options){this.target=target;this.options=options;}}
 const window={mostrarNotificacao(){}};
 const context={document,window,MutationObserver:Observer,WeakMap,WeakSet,Set,queueMicrotask,setTimeout(){},clearTimeout(){},Event};vm.createContext(context);
 vm.runInContext(script('validacaoInline.js'),context);vm.runInContext(script('pecaSeletor.js'),context);
 eventos.DOMContentLoaded.forEach(f=>f());
 window.validacaoInline.validar(select);window.validacaoInline.focar(select);await Promise.resolve();
 assert.equal(abrir.disabled,false);assert.equal(abrir.focado,true);assert.equal(select.focado,undefined);
 abrir.fire('click');assert.equal(painel.hidden,false);
 abrir.fire('click');assert.equal(painel.hidden,true);
 abrir.fire('click');assert.equal(painel.hidden,false);
 assert.deepEqual(Array.from(observadores[0].options.attributeFilter),['disabled']);
});

test('cadastro de impressora valida modelo antes de acessar potência oculta',async()=>{
 const eventos={},avisos=[];
 function campo(){const classes=new Set();return {value:'',dataset:{},type:'text',willValidate:true,validity:{valid:false,valueMissing:true},classList:{contains:()=>false,add:c=>classes.add(c),remove:c=>classes.delete(c)},classes,setAttribute(){},removeAttribute(){},focus(){},closest:()=>null};}
 const nome=campo(),modelo=campo(),potencia=campo();
 class Form {constructor(){this.method='post';this.elements=[nome,modelo,potencia];}querySelector(s){return s==='#nome'?nome:modelo;}}
 const form=new Form(),document={readyState:'loading',querySelectorAll:s=>s==='form'?[form]:[],addEventListener:(n,f)=>eventos[n]=f};
 const context={document,window:{mostrarNotificacao:m=>avisos.push(m)},HTMLFormElement:Form,WeakMap,Set,queueMicrotask,setTimeout(){},clearTimeout(){}};
 vm.runInNewContext(script('validacaoInline.js'),context);eventos.DOMContentLoaded();
 eventos.submit({target:form,preventDefault(){}});await Promise.resolve();
 assert.match(avisos[0],/modelo da impressora/);assert.equal(modelo.classes.has('campo-erro-temporario'),true);assert.equal(potencia.classes.size,0);
 assert.equal(nome.classes.has('campo-erro-temporario'),true);
 nome.value='Minha impressora';nome.validity={valid:true};
 modelo.value='Outro modelo';modelo.validity={valid:true};
 eventos.submit({target:form,preventDefault(){}});await Promise.resolve();
 assert.equal(potencia.classes.has('campo-erro-temporario'),true);
});
