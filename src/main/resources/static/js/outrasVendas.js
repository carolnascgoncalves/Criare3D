function iniciarEmbalagemOutrasVendas() {
    const form=document.querySelector('[data-venda-produto-form]');
    if(!form || form.dataset.embalagemIniciada) return;
    form.dataset.embalagemIniciada='true';
    const tipo=form.querySelector('[data-venda-embalagem-tipo]');
    const cadastro=form.querySelector('[data-venda-embalagem-cadastro]');
    const personalizado=form.querySelector('[data-venda-embalagem-valor]');
    function atualizar() {
        for(const [campo,ativo] of [[cadastro,tipo.value==='CADASTRADA'],[personalizado,tipo.value==='PERSONALIZADA'],[form.querySelector('[data-venda-embalagem-quantidade]'),tipo.value!=='SEM']]) {
            if(!campo)continue;
            campo.hidden=!ativo;const input=campo.querySelector('input,select');input.disabled=!ativo;input.required=ativo;
        }
    }
    const tentativa=form.querySelector('[data-restaurar-venda]');
    if(tentativa){
        tentativa.content.querySelectorAll('input').forEach(c=>{const destino=form.elements.namedItem(c.name);if(destino){if(destino.tagName==='SELECT'&&c.value&&![...destino.options].some(o=>o.value===c.value)){const opcao=document.createElement('option');opcao.value=c.value;opcao.textContent='Seleção anterior — confira no catálogo';destino.append(opcao);}destino.value=c.value;}});
        const dialogo=form.closest('dialog');if(dialogo&&!dialogo.open)dialogo.showModal();
    }
    tipo.addEventListener('change',atualizar);atualizar();
    document.addEventListener('turbo:before-cache',()=>{delete form.dataset.embalagemIniciada;},{once:true});
}
document.addEventListener('DOMContentLoaded',iniciarEmbalagemOutrasVendas);
document.addEventListener('turbo:load',iniciarEmbalagemOutrasVendas);
if(document.readyState==='interactive' || document.readyState==='complete') iniciarEmbalagemOutrasVendas();
