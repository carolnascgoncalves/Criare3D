(() => {
    function atualizar(root) {
        const formId = root.querySelector('[data-selecao-toggle]')?.dataset.form;
        const form = formId ? document.getElementById(formId) : null;
        const selecionados = root.querySelectorAll('.checkbox-selecao-listagem:checked');
        const remover = root.querySelector('[data-excluir-selecionados]');
        const contador = root.querySelector('[data-contador-selecao]');
        if (remover) {
            remover.hidden = !root.classList.contains('modo-selecao');
            remover.disabled = selecionados.length === 0;
        }
        if (contador) contador.textContent = `${selecionados.length} selecionado(s)`;
        if (form) form.dataset.quantidade = selecionados.length;
    }

    function inicializar() {
        document.querySelectorAll('[data-lista-selecao]').forEach(atualizar);
    }

    document.addEventListener('click', (event) => {
        const botao = event.target.closest('[data-selecao-toggle]');
        if (!botao) return;
        const root = botao.closest('[data-lista-selecao]');
        if (!root) return;
        const ativo = root.classList.toggle('modo-selecao');
        botao.innerHTML = ativo
            ? '<i class="bi bi-x-lg"></i> Cancelar seleção'
            : '<i class="bi bi-check2-square"></i> Selecionar';
        if (!ativo) root.querySelectorAll('.checkbox-selecao-listagem:checked').forEach((checkbox) => { checkbox.checked = false; });
        atualizar(root);
    });

    document.addEventListener('change', (event) => {
        if (!event.target.matches('.checkbox-selecao-listagem')) return;
        const root = event.target.closest('[data-lista-selecao]');
        if (root) atualizar(root);
    });

    document.addEventListener('DOMContentLoaded', inicializar);
    document.addEventListener('turbo:load', inicializar);
})();
