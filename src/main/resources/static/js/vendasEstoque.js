(() => {
    function inicializar() {
        document.querySelectorAll('[data-vendas-selecao]').forEach(root => {
            if (root.dataset.selecaoInicializada === 'true') return;
            root.dataset.selecaoInicializada = 'true';
            const alternar = root.querySelector('[data-vendas-alternar]');
            const conteudoSelecao = root.querySelector('[data-vendas-selecao-conteudo]');
            const todos = [...root.querySelectorAll('[data-selecionar-todos]')];
            const caixas = [...root.querySelectorAll('.checkbox-selecao-listagem')];
            const acoes = [...root.querySelectorAll('[data-vendas-acao]')];
            const contador = root.querySelector('[data-vendas-contador]');
            const quantidadeLote = root.querySelector('[data-vendas-quantidade]');
            const quantidadeForm = root.querySelector('[data-vendas-quantidade-form]');
            let modoSelecao = false;

            const atualizar = () => {
                const selecionados = caixas.filter(c => c.checked).length;
                const temVendas = caixas.some(c => c.checked && c.dataset.vendasRegistradas === 'true');
                const avisoProducao = root.querySelector('[data-vendas-bloqueio-producao]');
                if (avisoProducao) avisoProducao.hidden = !temVendas;
                acoes.forEach(botao => {
                    const bloquear = botao.value === 'DESFAZER_PRODUCAO' && temVendas;
                    botao.disabled = selecionados === 0 || bloquear;
                    botao.title = bloquear ? 'Há unidades vendidas neste lote. Cancele as vendas antes de desfazer a produção.' : '';
                });
                if (contador) contador.textContent = `${selecionados} selecionado(s)`;
                todos.forEach(caixa => {
                    caixa.checked = caixas.length > 0 && selecionados === caixas.length;
                    caixa.indeterminate = selecionados > 0 && selecionados < caixas.length;
                });
            };

            const sairDaSelecao = () => {
                modoSelecao = false;
                root.classList.remove('modo-selecao');
                alternar?.setAttribute('aria-pressed', 'false');
                if (alternar) alternar.innerHTML = '<i class="bi bi-check2-square" aria-hidden="true"></i> Selecionar';
                if (conteudoSelecao) conteudoSelecao.hidden = true;
                caixas.forEach(caixa => { caixa.checked = false; });
                atualizar();
            };

            alternar?.addEventListener('click', () => {
                modoSelecao = !modoSelecao;
                root.classList.toggle('modo-selecao', modoSelecao);
                alternar.setAttribute('aria-pressed', String(modoSelecao));
                alternar.innerHTML = modoSelecao
                    ? '<i class="bi bi-check2-square" aria-hidden="true"></i> Selecionando'
                    : '<i class="bi bi-check2-square" aria-hidden="true"></i> Selecionar';
                if (conteudoSelecao) conteudoSelecao.hidden = !modoSelecao;
                if (!modoSelecao) {
                    caixas.forEach(caixa => { caixa.checked = false; });
                    todos.forEach(caixa => { caixa.checked = false; caixa.indeterminate = false; });
                }
                atualizar();
            });

            root.querySelector('[data-vendas-sair]')?.addEventListener('click', sairDaSelecao);

            root.addEventListener('click', event => {
                const botao = event.target instanceof Element
                    ? event.target.closest('[data-vendas-acao]') : null;
                if (!botao) return;
                const form = document.getElementById(botao.getAttribute('form'));
                if (!form) return;
                form.dataset.turboConfirm = botao.dataset.turboConfirm || '';
                form.dataset.confirmLabel = botao.dataset.confirmLabel || 'Confirmar';
            });

            todos.forEach(seletorTodos => seletorTodos.addEventListener('change', () => {
                caixas.forEach(caixa => { caixa.checked = seletorTodos.checked; });
                atualizar();
            }));
            caixas.forEach(caixa => caixa.addEventListener('change', atualizar));
            quantidadeLote?.addEventListener('input', () => {
                if (quantidadeForm) quantidadeForm.value = quantidadeLote.value;
            });
            atualizar();
        });
    }

    document.addEventListener('DOMContentLoaded', inicializar);
    document.addEventListener('turbo:load', inicializar);
    if (document.readyState !== 'loading') inicializar();
})();
