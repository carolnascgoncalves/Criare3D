const {test}=require('node:test');const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');
test('pular simulação usa os términos calculados pelo servidor e não registra produção',()=>{
 const listeners={},nodes=Object.fromEntries(['progresso','prontas','estado','relogio','periodo'].map(n=>[`[data-sim-${n}]`,{}]));
 const root={dataset:{auto:'false'},isConnected:true,querySelector:s=>nodes[s],querySelectorAll:()=>[
  {dataset:{inicio:'2026-11-13T17:00:00',fim:'2026-11-13T19:00:00',unidades:'3',nome:'Suporte'}},
  {dataset:{inicio:'2026-11-16T08:00:00',fim:'2026-11-16T10:00:00',unidades:'2',nome:'Suporte'}}
 ]};
 const document={addEventListener:(n,f)=>listeners[n]=f,querySelectorAll:()=>[root]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidoSimulacao.js','utf8'),{document,window:{},Date,Intl,cancelAnimationFrame(){},requestAnimationFrame(){throw Error('Pular não deve animar');}});
 const botao={closest:()=>root};listeners.click({target:{closest:s=>s==='[data-sim-pular]'?botao:null}});
 assert.equal(nodes['[data-sim-prontas]'].textContent,'5');assert.equal(nodes['[data-sim-progresso]'].value,100);
 assert.equal(nodes['[data-sim-estado]'].textContent,'Previsão concluída');
 assert.match(nodes['[data-sim-relogio]'].textContent,/16\/11/);
});
test('prazo aparece imediatamente e repetir animação preserva as cores por peça',()=>{
 const listeners={},nodes=Object.fromEntries(['progresso','prontas','estado','relogio','periodo','item','continuar'].map(n=>[`[data-sim-${n}]`,{}]));let quadro,cor;
 nodes['.sim-mesa']={style:{setProperty:(n,v)=>cor=v}};
 const root={dataset:{auto:'true'},isConnected:true,querySelector:s=>nodes[s],querySelectorAll:()=>[
  {dataset:{inicio:'2026-11-12T10:00:00',fim:'2026-11-12T11:00:00',unidades:'1',nome:'Peça A',item:'A'}},
  {dataset:{inicio:'2026-11-12T11:00:00',fim:'2026-11-12T12:00:00',unidades:'2',nome:'Peça B',item:'B'}}
 ]};
 const document={addEventListener:(n,f)=>listeners[n]=f,querySelectorAll:()=>[root]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidoSimulacao.js','utf8'),{document,window:{},Date,Intl,performance:{now:()=>0},cancelAnimationFrame(){},requestAnimationFrame:f=>{quadro=f;return 1;}});
 listeners['DOMContentLoaded']();assert.equal(nodes['[data-sim-prontas]'].textContent,'3');listeners.click({target:{closest:s=>s==='[data-sim-repetir]'?{closest:()=>root}:null}});quadro(0);const primeira=cor;assert.equal(nodes['[data-sim-continuar]'].hidden,true);
 quadro(9000);assert.notEqual(cor,primeira);assert.match(nodes['[data-sim-item]'].textContent,/Peça B/);assert.equal(nodes['[data-sim-prontas]'].textContent,'1');
 quadro(12000);assert.equal(nodes['[data-sim-prontas]'].textContent,'3');assert.equal(nodes['[data-sim-continuar]'].hidden,false);
});
test('peças sequenciais compartilham uma barra e recebem resumo de horários e duração',()=>{
 const listeners={};function elemento(){return {style:{},children:[],appendChild(el){this.children.push(el);},replaceChildren(){this.children=[];},get lastElementChild(){return this.children.at(-1);}};}
 const linha=elemento(),resumo=elemento(),nodes=Object.fromEntries(['prontas','estado','relogio','periodo'].map(n=>[`[data-sim-${n}]`,{}]));nodes['[data-pedido-linha]']=linha;nodes['[data-sim-resumo]']=resumo;
 const root={dataset:{},isConnected:true,querySelector:s=>nodes[s],querySelectorAll:()=>[
  {dataset:{inicio:'2026-11-12T10:00:00',fim:'2026-11-12T11:00:00',unidades:'1',nome:'Peça A',item:'A'}},
  {dataset:{inicio:'2026-11-12T11:00:00',fim:'2026-11-12T12:00:00',unidades:'2',nome:'Peça B',item:'B'}}
 ]};
 const document={addEventListener:(n,f)=>listeners[n]=f,createElement:elemento,querySelectorAll:()=>[root]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidoSimulacao.js','utf8'),{document,window:{},Date,Intl,cancelAnimationFrame(){}});
 listeners.click({target:{closest:s=>s==='[data-sim-pular]'?{closest:()=>root}:null}});
 assert.equal(linha.children.length,1);const blocos=linha.children[0].children.filter(el=>el.className==='pedido-faixa-impressao');assert.equal(blocos.length,2);
 assert.notEqual(blocos[0].style.backgroundColor,blocos[1].style.backgroundColor);assert.equal(blocos[0].style.top,blocos[1].style.top);
 assert.equal(resumo.children.length,2);assert.equal(resumo.children[0].children[0].textContent,'Peça A');assert.match(resumo.children[1].children.at(-1).textContent,/Duração total: 1 h/);
});
test('voltar pelo Turbo recalcula a apresentação automática do prazo',()=>{
 const listeners={},root={dataset:{auto:'true'},hidden:false};
 const document={addEventListener:(n,f)=>listeners[n]=f,querySelectorAll:()=>[root]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidoSimulacao.js','utf8'),{document,window:{},cancelAnimationFrame(){}});
 listeners['turbo:before-cache']();assert.equal(root.hidden,true);assert.equal(root.dataset.auto,'true');
});

test('pausa mantém o vaso concluído e informa a próxima peça e horário',()=>{
 const listeners={},nodes=Object.fromEntries(['prontas','estado','relogio','periodo','item','item-label','espera'].map(n=>['[data-sim-'+n+']',{}]));let y,quadro;
 nodes['[data-pedido-corte]']={setAttribute:(nome,valor)=>y=valor};
 const root={dataset:{auto:'true'},isConnected:true,querySelector:s=>nodes[s],querySelectorAll:()=>[
 {dataset:{inicio:'2026-11-16T08:00:00',fim:'2026-11-16T09:00:00',unidades:'1',nome:'Peça A',item:'A'}},
 {dataset:{inicio:'2026-11-23T08:00:00',fim:'2026-11-23T09:00:00',unidades:'1',nome:'Peça B',item:'B'}}]};
 const document={addEventListener:(n,f)=>listeners[n]=f,querySelectorAll:()=>[root]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidoSimulacao.js','utf8'),{document,window:{},Date,Intl,performance:{now:()=>0},cancelAnimationFrame(){},requestAnimationFrame:f=>{quadro=f;}});
 listeners.DOMContentLoaded();listeners.click({target:{closest:s=>s==='[data-sim-repetir]'?{closest:()=>root}:null}});quadro(6000);
 assert.equal(nodes['[data-sim-espera]'].hidden,false);assert.match(nodes['[data-sim-espera]'].textContent,/Peça B.*23\/11/);
 assert.equal(nodes['[data-sim-item]'].textContent,'Peça A');assert.equal(nodes['[data-sim-item-label]'].textContent,'Última peça simulada');assert.equal(y,'44');
});
