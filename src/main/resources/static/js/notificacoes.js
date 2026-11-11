(() => {
    function ocultarDepois(notificacao, erro) {
        if (notificacao.dataset.temTemporizador === 'true') return;

        notificacao.dataset.temTemporizador = 'true';

        window.setTimeout(() => {
            notificacao.classList.add('alerta-configuracao-ocultando');

            window.setTimeout(() => {
                notificacao.remove();
            }, 350);
        }, erro ? 3500 : 1800);
    }

    function posicionarNotificacoes() {
        document.querySelectorAll('.notificacao-flutuante.alerta-erro, .notificacao-flutuante.alert-danger').forEach((aviso, indice) => { if (indice > 0) aviso.remove(); });
        const menu = document.querySelector('.menu-principal');
        const notificacoes = [
            ...document.querySelectorAll('.notificacao-flutuante')
        ];

        const bordaMenu = menu?.getBoundingClientRect().right || 0;
        const esquerda = Math.max(12, bordaMenu + 12);
        const largura = Math.max(0, Math.min(620, window.innerWidth - esquerda - 12));
        let topo = 12;

        notificacoes.forEach(notificacao => {
            notificacao.style.top = `${Math.ceil(topo)}px`;
            notificacao.style.left = `${Math.ceil(esquerda)}px`;
            notificacao.style.width = `${Math.floor(largura)}px`;
            notificacao.style.transform = 'none';

            topo += notificacao.getBoundingClientRect().height + 8;

            ocultarDepois(
                notificacao,
                notificacao.classList.contains('alerta-erro')
                || notificacao.classList.contains('alert-danger')
            );
        });
    }

    function destacarFalhasDeExclusao() {
        const bloqueado = new URLSearchParams(window.location.search).get('bloqueado');
        if (bloqueado) {
            document.querySelectorAll('.checkbox-selecao-listagem').forEach(caixa => {
                if (caixa.value === bloqueado) caixa.closest('.peca-lista-item, tr')?.classList.add('item-exclusao-falhou');
            });
        }
        const linhas = document.querySelectorAll('.item-exclusao-falhou');

        linhas.forEach(linha => {
            if (linha.dataset.destaqueAgendado === 'true') return;

            linha.dataset.destaqueAgendado = 'true';

            window.setTimeout(() => {
                linha.classList.add('item-exclusao-falhou-ocultando');

                window.setTimeout(() => {
                    linha.classList.remove(
                        'item-exclusao-falhou',
                        'item-exclusao-falhou-ocultando'
                    );

                    delete linha.dataset.destaqueAgendado;
                }, 400);

            }, 2200);
        });
    }

    window.mostrarNotificacao = (mensagem, tipo = 'erro') => {
        const main = document.querySelector('main.pagina');
        if (!main) return;

        if (tipo === 'erro') {
            document.querySelectorAll('.notificacao-flutuante.alerta-erro, .notificacao-flutuante.alert-danger').forEach(aviso => aviso.remove());
        }
        const aviso = document.createElement('div');

        aviso.className = `${tipo === 'info' ? 'aviso-info' : `alerta-${tipo}`} notificacao-flutuante`;
        aviso.setAttribute('role', tipo === 'erro' ? 'alert' : 'status');
        aviso.textContent = mensagem;

        main.prepend(aviso);
        posicionarNotificacoes();
    };

    function inicializar() {
        posicionarNotificacoes();
        destacarFalhasDeExclusao();
    }

    document.addEventListener('DOMContentLoaded', inicializar);
    document.addEventListener('transitionend', evento => {
        if (evento.target.matches?.('.menu-principal') && evento.propertyName === 'width') posicionarNotificacoes();
    });
    document.addEventListener('turbo:load', inicializar);

    window.addEventListener('resize', posicionarNotificacoes, {
        passive: true
    });
})();
