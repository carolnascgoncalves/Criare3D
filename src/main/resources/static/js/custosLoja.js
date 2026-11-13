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
    };
    tipo.addEventListener('change', atualizar); atualizar();
}
document.addEventListener('DOMContentLoaded', iniciarCustosLoja);
document.addEventListener('turbo:load', iniciarCustosLoja);
