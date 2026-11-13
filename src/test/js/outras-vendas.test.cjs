const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
test('embalagem habilita e exige apenas os campos da opção escolhida',()=>{
    const inputCadastro={},inputValor={};
    const cadastro={querySelector:()=>inputCadastro},valor={querySelector:()=>inputValor};
    let mudar;const tipo={value:'SEM',addEventListener:(n,f)=>{mudar=f;}};
    const form={dataset:{},querySelector:s=>({'[data-venda-embalagem-tipo]':tipo,'[data-venda-embalagem-cadastro]':cadastro,'[data-venda-embalagem-valor]':valor}[s])};
    const document={readyState:'complete',querySelector:()=>form,addEventListener(){}};
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/outrasVendas.js','utf8'),{document});
    assert.equal(cadastro.hidden,true);assert.equal(valor.hidden,true);assert.equal(inputValor.disabled,true);
    tipo.value='CADASTRADA';mudar();assert.equal(cadastro.hidden,false);assert.equal(inputCadastro.required,true);assert.equal(inputValor.disabled,true);
    tipo.value='PERSONALIZADA';mudar();assert.equal(valor.hidden,false);assert.equal(inputValor.required,true);assert.equal(inputCadastro.disabled,true);
    tipo.value='SEM';mudar();assert.equal(inputValor.required,false);assert.equal(inputCadastro.required,false);
});
