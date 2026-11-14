const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm');
function iniciar({seletores={},listas={},ids={}}={}) {
 const eventos={};
 const document={addEventListener:(n,f)=>(eventos[n]??=[]).push(f),querySelector:s=>seletores[s]??null,querySelectorAll:s=>listas[s]??[],getElementById:id=>ids[id]??null,createElement:tag=>no(tag)};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/polimento.js','utf8'),{window:{},document,location:{hash:''},setTimeout:()=>1,clearTimeout:()=>{},requestAnimationFrame:f=>f(),Date,Map,Number,Event:class{}});
 eventos.DOMContentLoaded.forEach(f=>f());return eventos;
}
function no(tag='div') {
 return {tag,children:[],dataset:{},style:{setProperty(k,v){this[k]=v}},attrs:{},eventos:{},addEventListener(n,f){this.eventos[n]=f},classList:{add(){},remove(){},toggle(){}},append(...n){this.children.push(...n)},setAttribute(k,v){this.attrs[k]=v},removeAttribute(k){delete this.attrs[k]}};
}
test('Acabando alterna estado e mantém somente saldos baixos visíveis',()=>{
 const linhas=[{dataset:{estoqueBaixo:'true'},hidden:false},{dataset:{estoqueBaixo:'false'},hidden:false}];
 const total={},legenda={},eventos={};
 const filtro={dataset:{},attrs:{'aria-pressed':'false'},querySelector:()=>total,addEventListener:(n,f)=>eventos[n]=f,getAttribute(k){return this.attrs[k]},setAttribute(k,v){this.attrs[k]=v}};
 iniciar({seletores:{'[data-filtrar-acabando]':filtro,'[data-estado-filtro-acabando]':legenda},listas:{'[data-estoque-baixo]':linhas}});
 assert.equal(total.textContent,'(1)');eventos.click();assert.equal(filtro.attrs['aria-pressed'],'true');assert.equal(linhas[1].hidden,true);assert.match(legenda.textContent,/Filtro ativo/);
 eventos.click();assert.equal(filtro.attrs['aria-pressed'],'false');assert.equal(linhas[1].hidden,false);
});
test('índice vai ao destino visível e marca o item ativo sem navegar pelo Turbo',()=>{
 const destino=no();destino.focus=o=>destino.foco=o;destino.scrollIntoView=o=>destino.rolagem=o;
 const indice=no('a');indice.getAttribute=()=> '#config-energia';
 const eventos=iniciar({listas:{'.configuracao-indice a':[indice]},ids:{'config-energia':destino}});
 let prevenido=false;
 eventos.click.forEach(f=>f({target:{closest:s=>s==='.configuracao-indice a[href^="#"]'?indice:null},preventDefault:()=>prevenido=true,stopPropagation(){}}));
 assert.equal(prevenido,true);assert.equal(indice.attrs['aria-current'],'location');assert.equal(destino.rolagem.block,'start');assert.equal(destino.foco.preventScroll,true);
});
test('nome completo está em toda faixa e legenda separa nome e datas',()=>{
 const agenda=no(),lista=no();
 const card={dataset:{agendaInicio:'2026-11-20T08:00:00',agendaFim:'2026-11-20T09:00:00',agendaImpressora:'1',agendaNome:'Impressora'},querySelector:s=>s==='h2'?{textContent:'Book Page Holder V3'}:{href:'/pedido/1'}};
 lista.querySelectorAll=()=>[card];
 iniciar({seletores:{'[data-agenda]':agenda,'[data-agenda-lista]':lista}});
 const artigo=agenda.children[1].children[1],marcos=artigo.children[0],faixa=artigo.children[1],legenda=artigo.children[2];
 assert.equal(marcos.children.length,2);assert.equal(marcos.children[0].textContent,"20/11");assert.equal(marcos.children[1].style.left,"100%");assert.equal(marcos.children[1].style['--marco-deslocamento'],'-100%');
 assert.match(artigo.title,/Book Page Holder V3/);assert.equal(faixa.title,artigo.title);assert.equal(legenda.children[0].textContent,'Book Page Holder V3');
 assert.match(legenda.children[1].children[0].textContent,/Início:/);assert.match(legenda.children[1].children[1].textContent,/Previsão:/);
});

test('calendário marca ocupação parcial e seleciona o dia preservando o horário',()=>{
 const area=no();area.replaceChildren=function(){this.children=[];};
 const cal=no();cal.querySelector=s=>s==='[data-calendario-conteudo]'?area:null;cal.querySelectorAll=()=>[{dataset:{inicio:'2026-11-20T10:00:00',fim:'2026-11-21T00:00:00',nome:'Impressora · Peça',impressoraId:'1',impressora:'Casa'}},{dataset:{inicio:'2026-11-20T12:00:00',fim:'2026-11-20T14:00:00',nome:'Segunda · Peça',impressoraId:'2',impressora:'Loja'}}];
 const input={value:'2026-11-20T08:30',addEventListener(){},dispatchEvent(){this.alterado=true;}};
 iniciar({seletores:{'[name=inicioPretendido]':input},listas:{'[data-calendario-ocupacao]':[cal]}});
 const dias=area.children[1].children.filter(e=>e.tag==='button');
 assert.match(dias[19].style.background,/linear-gradient/);assert.match(dias[19].title,/Ocupado/);assert.doesNotMatch(dias[20].title,/Ocupado/);
 assert.equal(dias[19].attrs['aria-pressed'],'true');
 dias[21].eventos.click();assert.equal(input.value,'2026-11-22T08:30');assert.equal(input.alterado,true);
});

test('Registrar venda mantém produto escolhido fora das primeiras opções do catálogo',()=>{
 const select=no('select');select.options=[];select.append=o=>select.options.push(o);select.dispatchEvent=()=>{};
 const dialogo={querySelector:()=>select,showModal(){this.aberto=true;}};
 const abrir={dataset:{dialogoAbrir:'vendaDialogo',produtoVender:'produto-21',produtoNome:'Produto da segunda página'}};
 const eventos=iniciar({ids:{vendaDialogo:dialogo}});
 eventos.click.forEach(f=>f({target:{closest:s=>s==='[data-dialogo-abrir]'?abrir:null}}));
 assert.equal(select.value,'produto-21');assert.equal(select.options[0].textContent,'Produto da segunda página');assert.equal(dialogo.aberto,true);
});
