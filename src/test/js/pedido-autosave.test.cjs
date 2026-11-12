const {test}=require('node:test');const assert=require('node:assert/strict');const fs=require('node:fs');const vm=require('node:vm');
function ambiente(fetch){const listeners={},mensagens=[];const status={classList:{toggle(){}},set textContent(v){mensagens.push(v);}};const document={addEventListener:(n,f)=>listeners[n]=f,querySelectorAll:s=>s.includes('save-status')?[status]:[],querySelector:()=>null};const form={dataset:{autosaveUrl:'/teste'},isConnected:true,valido:true,valor:'10',checkValidity(){return this.valido;},reportValidity(){this.reportado=true;},closest:()=>({}),requestSubmit(){this.submits=(this.submits||0)+1;listeners.submit({target:this,preventDefault(){},stopImmediatePropagation(){}});}};vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidos.js','utf8').slice(fs.readFileSync('src/main/resources/static/js/pedidos.js','utf8').indexOf('// Serializa os salvamentos')),{window:{},document,fetch,setTimeout,clearTimeout,location:{pathname:'/orcamentos/teste'},Event:class{constructor(type){this.type=type;}},DOMParser:class{parseFromString(){return {querySelector:s=>s==='#pedidoPrazo'?{childNodes:['prazo novo']}:null};}},FormData:class{constructor(f){this.valor=f.valor;}},Intl,Promise,Map,Set,WeakSet,Error});return {form,listeners,mensagens,document,change(){listeners.change({target:{closest:()=>form}});}};}
const resposta=()=>({ok:true,headers:{get:()=> 'application/json'},json:async()=>({total:20,revisao:1,itens:[]})});const tick=()=>new Promise(setImmediate);
test('autosaves são serializados e preservam os valores de cada alteração',async()=>{let liberar;const recebidos=[];const a=ambiente(async(url,r)=>{recebidos.push(r.body.valor);if(recebidos.length===1)await new Promise(r=>liberar=r);return resposta();});a.change();await tick();a.form.valor='15';a.change();await tick();assert.deepEqual(recebidos,['10']);liberar();await tick();await tick();assert.deepEqual(recebidos,['10','15']);assert.equal(a.form.valor,'15');assert.equal(a.mensagens.at(-1),'Alterações salvas');});
test('campo inválido não dispara requisição',async()=>{let chamadas=0;const a=ambiente(async()=>{chamadas++;return resposta();});a.form.valido=false;a.change();await tick();assert.equal(chamadas,0);});
test('falha mantém o campo e avisa sem navegar',async()=>{const a=ambiente(async()=>{throw Error('Sem conexão');});a.change();await tick();assert.equal(a.form.valor,'10');assert.equal(a.mensagens.at(-1),'Sem conexão');});

test('editar o início agenda salvamento antes de sair do campo e atualiza a prévia',async()=>{
 const chamadas=[];let conteudo,evento;const nova={childNodes:['prazo novo']};
 const a=ambiente(async(url)=>{chamadas.push(url);return url==='/teste'?resposta():{ok:true,text:async()=>'<html></html>'};});
 a.form.dataset.etapa='2';a.document.querySelector=s=>s==='#pedidoPrazo'?{replaceChildren:(...nodes)=>conteudo=nodes}:null;
 a.document.dispatchEvent=e=>evento=e.type;
 // O debounce é testado com relógio real; o campo mantém o foco (nenhum blur/change).
 // A resposta atualiza apenas a prévia e mantém o formulário no lugar.
 a.listeners.input({target:{name:'inicioPretendido',matches:()=>false,closest:()=>a.form}});
 await new Promise(r=>setTimeout(r,450));await tick();assert.deepEqual(chamadas,['/teste','/orcamentos/teste?etapa=2']);assert.deepEqual(conteudo,['prazo novo']);assert.equal(evento,'pedido:preview');
});
