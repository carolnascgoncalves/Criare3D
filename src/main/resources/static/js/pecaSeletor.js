(() => {
    const inicializados = new WeakSet();
    const normalizar = texto => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');
    const numero = valor => Number(valor || 0).toLocaleString('pt-BR', { maximumFractionDigits: 2 });

    function iniciar() {
        const root = document.querySelector('[data-peca-seletor]');
        const select = document.getElementById('pecaId');
        if (!root || !select || inicializados.has(root)) return;
        inicializados.add(root);
        const abrir = root.querySelector('[data-peca-abrir]');
        const painel = root.querySelector('[data-peca-painel]');
        const busca = root.querySelector('[data-peca-busca]');
        const lista = root.querySelector('[data-peca-lista]');
        const vazio = root.querySelector('[data-peca-vazio]');
        const resumo = opcao => `${numero(opcao.dataset.gramas)} g · ${numero(opcao.dataset.tempo)} min${opcao.dataset.temArquivo === 'true' ? ' · Arquivo fatiado' : ''}`;
        const fechar = () => { painel.hidden = true; abrir.setAttribute('aria-expanded', 'false'); };
        function atualizar() {
            const opcao = select.selectedOptions[0];
            root.querySelector('[data-peca-nome]').textContent = select.value ? opcao.textContent.trim() : 'Escolha uma peça';
            root.querySelector('[data-peca-resumo]').textContent = select.value ? resumo(opcao) : 'Busque no catálogo';
            abrir.disabled = select.disabled;
            abrir.setAttribute('aria-invalid', String(select.getAttribute('aria-invalid') === 'true'));
        }
        function renderizar() {
            const termo = normalizar(busca.value.trim());
            const fragmento = document.createDocumentFragment();
            [...select.options].filter(opcao => opcao.value && normalizar(opcao.textContent).includes(termo)).forEach(opcao => {
                const botao = document.createElement('button');
                botao.type = 'button';
                botao.className = 'sim-seletor-item';
                botao.setAttribute('role', 'option');
                botao.setAttribute('aria-selected', String(opcao.value === select.value));
                const icone = document.createElement('span');
                icone.className = 'sim-seletor-sem-foto';
                icone.innerHTML = '<i class="bi bi-box" aria-hidden="true"></i>';
                const textos = document.createElement('span');
                textos.className = 'sim-seletor-textos';
                const nome = document.createElement('strong');
                nome.className = 'sim-seletor-nome';
                nome.textContent = opcao.textContent.trim();
                const detalhe = document.createElement('span');
                detalhe.className = 'sim-seletor-resumo';
                detalhe.textContent = resumo(opcao);
                textos.append(nome, detalhe);
                botao.append(icone, textos);
                botao.addEventListener('click', () => {
                    if (select.disabled) return;
                    select.value = opcao.value;
                    select.dispatchEvent(new Event('change', { bubbles: true }));
                    atualizar();
                    fechar();
                    abrir.focus();
                });
                fragmento.appendChild(botao);
            });
            vazio.hidden = fragmento.childElementCount > 0;
            lista.replaceChildren(fragmento);
        }
        abrir.addEventListener('click', () => {
            if (!painel.hidden) return fechar();
            busca.value = '';
            renderizar();
            painel.hidden = false;
            abrir.setAttribute('aria-expanded', 'true');
            busca.focus();
        });
        busca.addEventListener('input', renderizar);
        root.addEventListener('keydown', event => {
            if (event.key === 'Escape') { fechar(); abrir.focus(); event.preventDefault(); }
            if (painel.hidden || !['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
            const itens = [...lista.querySelectorAll('[role="option"]')];
            if (!itens.length) return;
            let indice = itens.indexOf(document.activeElement);
            if (event.key === 'Home') indice = 0;
            else if (event.key === 'End') indice = itens.length - 1;
            else indice = (indice + (event.key === 'ArrowDown' ? 1 : -1) + itens.length) % itens.length;
            itens[indice].focus();
            event.preventDefault();
        });
        document.addEventListener('click', event => { if (root.isConnected && !root.contains(event.target)) fechar(); });
        root.querySelector('[data-peca-cadastrar]').addEventListener('click', () => window.salvarRascunhoCalculo?.());
        select.addEventListener('change', atualizar);
        select.addEventListener('pecaSelecionada', atualizar);
        select.addEventListener('focus', () => abrir.focus());
        select.addEventListener('invalid', event => { event.preventDefault(); abrir.setAttribute('aria-invalid', 'true'); });
        const observador = new MutationObserver(atualizar);
        observador.observe(select, { attributes: true, attributeFilter: ['disabled', 'aria-invalid'] });
        select.classList.add('sim-seletor-nativo');
        select.tabIndex = -1;
        select.setAttribute('aria-hidden', 'true');
        root.hidden = false;
        atualizar();
    }
    document.addEventListener('DOMContentLoaded', iniciar);
    document.addEventListener('turbo:load', iniciar);
    if (document.readyState !== 'loading') iniciar();
})();
