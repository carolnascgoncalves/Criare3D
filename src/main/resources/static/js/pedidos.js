(() => {
    if (window.criarePedidosInicializado) return;
    window.criarePedidosInicializado = true;
    document.addEventListener('error', event => {
        if (event.target.matches?.('[data-pedido-foto]')) event.target.remove();
    }, true);
    document.addEventListener('input', event => {
        if (!event.target.matches('[data-pedido-busca]')) return;
        const form = event.target.closest('form');
        const normalizar = valor => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
        const termo = normalizar(event.target.value);
        let visiveis = 0;
        form.querySelectorAll('[data-pedido-referencia]').forEach(linha => {
            linha.hidden = !normalizar(linha.dataset.pedidoReferencia).includes(termo);
            if (!linha.hidden) visiveis++;
        });
        const vazio = form.querySelector('[data-pedido-sem-resultados]');
        if (vazio) vazio.hidden = visiveis > 0;
    });
    function ajustarEstoque(form) {
        const selecionado = form.querySelector('input[name="referencia"]:checked');
        const estoque = form.querySelector('[data-pedido-estoque]');
        const quantidade = form.querySelector('[name="quantidade"]');
        if (!estoque) return;
        estoque.max = String(Math.max(0, Math.min(Number(selecionado?.dataset.disponivel ?? form.dataset?.disponivel ?? 0), Number(quantidade?.value || 0))));
        if (Number(estoque.value) > Number(estoque.max)) estoque.value = estoque.max;
        estoque.readOnly = Number(estoque.max) === 0;
        const titulo = form.querySelector('[data-pedido-selecionada]');
        if (titulo && selecionado) titulo.textContent = selecionado.dataset.nome || 'Peça selecionada';
        const resumo = form.querySelector('[data-pedido-origem]');
        if (resumo) {
            const total = Math.max(0, Number(quantidade?.value || 0));
            const prontas = Math.max(0, Math.min(Number(estoque.value || 0), Number(estoque.max)));
            resumo.textContent = selecionado
                ? `Pedido: ${total} un. — ${prontas} do estoque e ${Math.max(0, total - prontas)} para produzir. Neste lote há ${Number(selecionado.dataset.disponivel || 0)} un. livres.`
                : 'Escolha uma peça para conferir o estoque e a quantidade a produzir.';
        }
    }
    document.addEventListener('change', event => {
        const form = event.target.closest('.pedido-adicionar, .pedido-editar-item');
        if (form) {
            ajustarEstoque(form);

        }
    });
    document.addEventListener('input', event => {
        const form = event.target.closest('.pedido-adicionar, .pedido-editar-item');
        if (form && event.target.matches('[name="quantidade"], [data-pedido-estoque]')) ajustarEstoque(form);
    });
    document.addEventListener('turbo:load', () => document.querySelectorAll('.pedido-adicionar, .pedido-editar-item').forEach(ajustarEstoque));
    document.addEventListener('DOMContentLoaded', () => document.querySelectorAll('.pedido-adicionar, .pedido-editar-item').forEach(ajustarEstoque));
    function multi(row) {
        const marcado=row.querySelector('[data-pedido-marcar]');const campos=row.querySelector('[data-pedido-multi-campos]');const quantidade=row.querySelector('[data-multi-quantidade]');const estoque=row.querySelector('[data-multi-estoque]');
        campos.hidden=!marcado.checked;quantidade.disabled=estoque.disabled=!marcado.checked;
        estoque.max=String(Math.max(0,Math.min(Number(marcado.dataset.disponivel),Number(quantidade.value||0))));
        estoque.readOnly=Number(estoque.max)===0;if(Number(estoque.value)>Number(estoque.max))estoque.value=estoque.max;
    }
    document.addEventListener('change',e=>{const row=e.target.closest('[data-pedido-multi]');if(row)multi(row);});
    document.addEventListener('input',e=>{const row=e.target.closest('[data-pedido-multi]');if(row)multi(row);});
    document.addEventListener('turbo:load',()=>document.querySelectorAll('[data-pedido-multi]').forEach(multi));
    document.addEventListener('DOMContentLoaded',()=>document.querySelectorAll('[data-pedido-multi]').forEach(multi));
    document.addEventListener('click', async event => {
        if (event.target.closest('[data-pedido-imprimir]')) window.print();
        const copiar = event.target.closest('[data-pedido-copiar]');
        if (!copiar) return;
        try {
            await navigator.clipboard.writeText(document.querySelector('[data-pedido-proposta]').innerText);
            copiar.textContent = 'Mensagem copiada';
        } catch { copiar.textContent = 'Selecione e copie o texto da proposta'; }
    });
})();
