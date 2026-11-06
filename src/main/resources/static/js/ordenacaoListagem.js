(() => {
    const formulariosInicializados = new WeakSet();

    function inicializarOrdenacao() {
        document.querySelectorAll('form[data-ordenacao-listagem]').forEach(formulario => {
            if (formulariosInicializados.has(formulario)) return;

            const campo = formulario.querySelector('[data-ordenacao-campo]');
            const ordem = formulario.querySelector('[data-ordenacao-valor]');
            const botaoDirecao = formulario.querySelector('[data-ordenacao-direcao]');
            const textoDirecao = formulario.querySelector('[data-ordenacao-status]');
            const iconeDirecao = formulario.querySelector('[data-ordenacao-icone]');
            const botaoAplicar = formulario.querySelector('[data-ordenacao-aplicar]');

            if (!campo || !ordem || !botaoDirecao) return;

            formulariosInicializados.add(formulario);

            const ordemAplicada = ordem.value;
            let direcao = ordem.value.endsWith('Desc') ? 'Desc' : 'Asc';

            const sufixoAtual = direcao;
            const campoAtual = ordem.value.slice(0, -sufixoAtual.length);

            if ([...campo.options].some(option => option.value === campoAtual)) {
                campo.value = campoAtual;
            }

            function atualizarInterface() {
                ordem.value = campo.value + direcao;

                const crescente = direcao === 'Asc';

                if (iconeDirecao) {
                    iconeDirecao.className = crescente
                        ? 'bi bi-arrow-up'
                        : 'bi bi-arrow-down';
                }

                if (textoDirecao) {
                    textoDirecao.textContent = crescente
                        ? 'Crescente'
                        : 'Decrescente';
                }

                botaoDirecao.setAttribute(
                    'aria-label',
                    crescente
                        ? 'Ordenação crescente. Clique para inverter.'
                        : 'Ordenação decrescente. Clique para inverter.'
                );

                botaoDirecao.title = crescente
                    ? 'Ordenação crescente'
                    : 'Ordenação decrescente';

                if (botaoAplicar) {
                    const alterada = ordem.value !== ordemAplicada;

                    botaoAplicar.classList.toggle(
                        'ordenacao-aplicar-pendente',
                        alterada
                    );

                    botaoAplicar.setAttribute(
                        'aria-label',
                        alterada
                            ? 'Aplicar nova ordenação'
                            : 'Aplicar ordenação'
                    );
                }
            }

            campo.addEventListener('change', atualizarInterface);

            botaoDirecao.addEventListener('click', () => {
                direcao = direcao === 'Asc' ? 'Desc' : 'Asc';
                atualizarInterface();
            });

            formulario.addEventListener('submit', () => {
                ordem.value = campo.value + direcao;
            });

            atualizarInterface();
        });
    }

    document.addEventListener('DOMContentLoaded', inicializarOrdenacao);
    document.addEventListener('turbo:load', inicializarOrdenacao);
})();