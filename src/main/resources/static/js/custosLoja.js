function iniciarCustosLoja() {
    const tipo = document.querySelector('[data-custo-tipo]');
    if (!tipo || tipo.dataset.iniciado) return;
    tipo.dataset.iniciado = 'true';
    const atualizar = () => {
        const mensal = tipo.value === 'RATEIO_MENSAL';
        const valor = document.querySelector('[data-custo-valor]');
        const unidades = document.querySelector('[data-custo-unidades]');
        valor.hidden = mensal; unidades.hidden = !mensal;
        valor.querySelector('input').disabled = mensal;
        valor.querySelector('input').required = !mensal;
        valor.querySelector('input').max = tipo.value === 'PERCENTUAL' ? '100' : '9999999999.99';
        unidades.querySelector('input').disabled = !mensal;
        unidades.querySelector('input').required = mensal;
        const rotulo=document.querySelector('[data-custo-valor-rotulo]');
        if(rotulo) rotulo.textContent=tipo.value==='PERCENTUAL' ? 'Qual percentual acrescentar ao custo? (%)' : 'Quanto acrescentar por peça? (R$)';
        const preview=document.querySelector('[data-custo-preview]');
        if(preview) {
            const meta=Number(document.getElementById('meta').value), qtd=Number(document.getElementById('unidadesPrevistas').value);
            const valor=Number(document.getElementById('valor').value);
            const dinheiro=v=>v.toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
            preview.textContent=mensal ? (meta>0 && qtd>0 ? `Isso soma ${dinheiro(meta/qtd)} em cada peça.` : 'Informe a meta e quantas peças pretende vender para conferir o valor por peça.') : tipo.value==='PERCENTUAL' ? `Isso acrescenta ${valor || 0}% ao custo de cada peça, antes do lucro.` : `Isso soma ${dinheiro(valor)} em cada peça.`;
        }
    };
    tipo.addEventListener('change', atualizar); tipo.closest('form').addEventListener('input',atualizar); atualizar();
}
document.addEventListener('DOMContentLoaded', iniciarCustosLoja);
document.addEventListener('turbo:load', iniciarCustosLoja);
