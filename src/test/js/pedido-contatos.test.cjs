const {test}=require('node:test');const assert=require('node:assert/strict');const fs=require('node:fs');const vm=require('node:vm');
test('contato gera somente o destino válido e não abre nem envia nada automaticamente',()=>{
 const listeners={},input={value:'(11) 99999-1234'};
 const links=['whatsapp','email'].map(tipo=>({dataset:{pedidoDestino:tipo},attrs:{},setAttribute(n,v){this.attrs[n]=v;}}));
 const caixa={dataset:{contato:'',titulo:'Orçamento exemplo',total:'20,00'},querySelectorAll:()=>links};
 const document={addEventListener:(n,f)=>listeners[n]=f,querySelector:s=>s.includes('input')?input:null,querySelectorAll:()=>[caixa]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidoContatos.js','utf8'),{window:{},document,encodeURIComponent});
 listeners['DOMContentLoaded']();assert.match(links[0].href,/https:\/\/wa.me\/5511999991234\?text=/);assert.equal(links[1].href,'#');
 input.value='cliente@example.com';listeners.input({target:{name:'contato'}});
 assert.match(links[1].href,/^mailto:/);assert.equal(links[0].href,'#');
 input.value='javascript:alert(1)';listeners.input({target:{name:'contato'}});
 assert.equal(links[0].href,'#');assert.equal(links[1].href,'#');
});
test('Gmail abre composição no navegador com destinatário e mensagem codificados',()=>{
 const listeners={},link={dataset:{pedidoDestino:'gmail'},setAttribute(){}};
 const caixa={dataset:{contato:'cliente@example.com',titulo:'Proposta',total:'15,00'},querySelectorAll:s=>s.includes('item-texto')?[]:[link]};
 const document={addEventListener:(n,f)=>listeners[n]=f,querySelector:()=>null,querySelectorAll:()=>[caixa]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidoContatos.js','utf8'),{window:{},document,encodeURIComponent});listeners['DOMContentLoaded']();
 const url=new URL(link.href);assert.equal(url.origin,'https://mail.google.com');assert.equal(url.searchParams.get('to'),'cliente@example.com');assert.match(url.searchParams.get('body'),/Total: R\$ 15,00/);
});
