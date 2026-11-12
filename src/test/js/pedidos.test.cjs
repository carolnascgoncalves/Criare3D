const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
function carregar() {
 const listeners={};let impressoes=0;
 const document={addEventListener:(nome,fn)=>{(listeners[nome]??=[]).push(fn);},querySelectorAll:()=>[]};
 vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/pedidos.js','utf8'),{document,window:{print:()=>impressoes++},navigator:{}});
 return {listeners,document,impressoes:()=>impressoes};
}
test('estoque acompanha o menor valor entre saldo livre e quantidade solicitada',()=>{
 const {listeners}=carregar();
 const estoque={value:'5'},quantidade={value:'3'},selecionado={dataset:{disponivel:'10'}};
 const form={querySelector:s=>s.includes(':checked')?selecionado:s.includes('estoque')?estoque:quantidade};
 const target={closest:()=>form};listeners.change[0]({target});
 assert.equal(estoque.max,'3');assert.equal(estoque.value,'3');
 selecionado.dataset.disponivel='1';listeners.change[0]({target});
 assert.equal(estoque.max,'1');assert.equal(estoque.value,'1');
});
test('busca ignora acentos e informa quando não há referências',()=>{
 const {listeners}=carregar();
 const linhas=[{dataset:{pedidoReferencia:'Peça decoração PLA'}},{dataset:{pedidoReferencia:'Suporte ABS'}}],vazio={hidden:true};
 const form={querySelectorAll:()=>linhas,querySelector:()=>vazio};
 const target={matches:()=>true,value:'decoracao',closest:()=>form};
 listeners.input[0]({target});assert.equal(linhas[0].hidden,false);assert.equal(linhas[1].hidden,true);assert.equal(vazio.hidden,true);
 target.value='inexistente';listeners.input[0]({target});assert.equal(vazio.hidden,false);
});
test('peça sem estoque zera o uso de prontas e impede editar esse campo',()=>{
 const {listeners}=carregar();const estoque={value:'1'},quantidade={value:'2'};
 const form={querySelector:s=>s.includes(':checked')?{dataset:{disponivel:'0'}}:s.includes('estoque')?estoque:s.includes('quantidade')?quantidade:null};
 listeners.change[0]({target:{closest:()=>form}});
 assert.equal(estoque.value,'0');assert.equal(estoque.max,'0');assert.equal(estoque.readOnly,true);
});
test('seleção múltipla habilita só os campos marcados e respeita o estoque de cada peça',()=>{
 const {listeners,document}=carregar();const checkbox={checked:true,dataset:{disponivel:'2'}},campos={},quantidade={value:'3'},estoque={value:'3'};
 const row={querySelector:s=>s.includes('marcar')?checkbox:s.includes('campos')?campos:s.includes('quantidade')?quantidade:estoque};
 document.querySelectorAll=()=>[row];listeners['DOMContentLoaded'].at(-1)();
 assert.equal(campos.hidden,false);assert.equal(quantidade.disabled,false);assert.equal(estoque.max,'2');assert.equal(estoque.value,'2');
 checkbox.checked=false;listeners['DOMContentLoaded'].at(-1)();assert.equal(campos.hidden,true);assert.equal(quantidade.disabled,true);assert.equal(estoque.disabled,true);
});
test('resumo explica quanto vem do estoque e quanto ainda precisa produzir',()=>{
 const {listeners}=carregar();
 const estoque={value:'2'},quantidade={value:'5'},selecionado={dataset:{disponivel:'3'}},resumo={};
 const form={querySelector:s=>s.includes(':checked')?selecionado:s.includes('origem')?resumo:s.includes('estoque')?estoque:quantidade};
 listeners.change[0]({target:{closest:()=>form}});
 assert.match(resumo.textContent,/5 un.*2 do estoque e 3 para produzir/);
 assert.match(resumo.textContent,/3 un. livres/);
});
test('foto ausente preserva o ícone e imprimir depende de clique explícito',async()=>{
 const c=carregar();let removida=false;
 c.listeners.error[0]({target:{matches:()=>true,remove:()=>removida=true}});
 assert.equal(removida,true);assert.equal(c.impressoes(),0);
 await c.listeners.click[0]({target:{closest:s=>s.includes('imprimir')?{}:null}});
 assert.equal(c.impressoes(),1);
});
