(() => {
    function alternarModoSelecao(botao) {
        const root = botao.closest('[data-lista-selecao]');
        if (!root) return;

        const ativo = !root.classList.contains('modo-selecao');
        root.classList.toggle('modo-selecao', ativo);

        botao.innerHTML = ativo
            ? '<i class="bi bi-x-lg" aria-hidden="true"></i> Cancelar seleção'
            : '<i class="bi bi-check2-square" aria-hidden="true"></i> Selecionar';

        if (!ativo) {
            root.querySelectorAll('.checkbox-selecao-listagem:checked')
                .forEach(checkbox => { checkbox.checked = false; });
        }

        atualizar(root);
    }

    function atualizar(root) {
        const botao = root.querySelector('[data-selecao-toggle]');
        const formId = botao?.dataset.form;
        const form = formId ? document.getElementById(formId) : null;

        const selecionados = root.querySelectorAll(
            '.checkbox-selecao-listagem:checked'
        );

        const contador = root.querySelector('[data-contador-selecao]');
        const modoAtivo = root.classList.contains('modo-selecao');

        if (botao) {
            botao.setAttribute('aria-pressed', String(modoAtivo));
        }

        root.querySelectorAll('[data-excluir-selecionados], [data-arquivar-selecionados]').forEach(acao => {
            acao.hidden = !modoAtivo;
            acao.disabled = selecionados.length === 0;
        });

        if (contador) {
            contador.textContent = `${selecionados.length} selecionado(s)`;
            contador.hidden = !modoAtivo;
        }

        if (form) {
            form.dataset.quantidade = selecionados.length;
        }
    }

    function inicializar() {
        document.querySelectorAll('[data-lista-selecao]').forEach(root => {
            const botao = root.querySelector('[data-selecao-toggle]');
            if (botao && botao.dataset.selecaoInicializada !== 'true') {
                botao.dataset.selecaoInicializada = 'true';
                botao.addEventListener('click', event => {
                    event.preventDefault();
                    alternarModoSelecao(botao);
                });
            }
            atualizar(root);
        });
    }

    document.addEventListener('change', event => {
        if (!(event.target instanceof Element)) return;
        if (!event.target.matches('.checkbox-selecao-listagem')) return;

        const root = event.target.closest('[data-lista-selecao]');
        if (root) atualizar(root);
    });

    document.addEventListener('DOMContentLoaded', inicializar);
    document.addEventListener('turbo:load', inicializar);
    if (document.readyState !== 'loading') inicializar();
})();
