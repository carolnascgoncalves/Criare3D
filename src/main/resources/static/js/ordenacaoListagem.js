(() => {
    function inicializarOrdenacao() {
        document.querySelectorAll('form[data-ordenacao-listagem]').forEach(formulario => {
            if (formulario.dataset.ordenacaoInicializada === 'true') return;

            const campo = formulario.querySelector('[data-ordenacao-campo]');
            const ordem = formulario.querySelector('[data-ordenacao-valor]');
            const botao = formulario.querySelector('[data-ordenacao-direcao]');

            if (!campo || !ordem || !botao) return;

            formulario.dataset.ordenacaoInicializada = 'true';

            function atualizarBotao() {
                const crescente = ordem.value.endsWith('Asc');

                botao.dataset.direcao = crescente ? 'Asc' : 'Desc';
                botao.innerHTML = crescente
                    ? '<i class="bi bi-sort-up"></i> Crescente'
                    : '<i class="bi bi-sort-down"></i> Decrescente';

                botao.title = crescente
                    ? 'Ordenação crescente. Clique para inverter.'
                    : 'Ordenação decrescente. Clique para inverter.';
            }

            function atualizarOrdem() {
                const direcao = ordem.value.endsWith('Desc') ? 'Desc' : 'Asc';
                ordem.value = campo.value + direcao;
                atualizarBotao();
            }

            campo.addEventListener('change', atualizarOrdem);

            botao.addEventListener('click', () => {
                const direcaoAtual = ordem.value.endsWith('Desc') ? 'Desc' : 'Asc';
                const novaDirecao = direcaoAtual === 'Asc' ? 'Desc' : 'Asc';

                ordem.value = campo.value + novaDirecao;
                formulario.requestSubmit();
            });

            formulario.addEventListener('submit', atualizarOrdem);

            const ordemAtual = ordem.value;
            const campoAtual = ordemAtual.endsWith('Desc')
                ? ordemAtual.slice(0, -4)
                : ordemAtual.slice(0, -3);

            if ([...campo.options].some(option => option.value === campoAtual)) {
                campo.value = campoAtual;
            }

            atualizarBotao();
        });
    }

    document.addEventListener('DOMContentLoaded', inicializarOrdenacao);
    document.addEventListener('turbo:load', inicializarOrdenacao);
})();