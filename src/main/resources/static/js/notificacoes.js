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
        const menu = document.querySelector('.menu-principal');
        const notificacoes = [
            ...document.querySelectorAll('.notificacao-flutuante')
        ];

        let topo = (menu?.getBoundingClientRect().height || 76) + 12;

        notificacoes.forEach(notificacao => {
            notificacao.style.top = `${Math.ceil(topo)}px`;

            topo += notificacao.getBoundingClientRect().height + 8;

            ocultarDepois(
                notificacao,
                notificacao.classList.contains('alerta-erro')
                || notificacao.classList.contains('alert-danger')
            );
        });
    }

    function destacarFalhasDeExclusao() {
        const linhas = document.querySelectorAll('.item-exclusao-falhou');

        linhas.forEach(linha => {
            if (linha.dataset.destaqueAgendado === 'true') return;

            linha.dataset.destaqueAgendado = 'true';

            window.setTimeout(() => {
                linha.classList.remove('item-exclusao-falhou');
                delete linha.dataset.destaqueAgendado;
            }, 1500);
        });
    }

    window.mostrarNotificacao = (mensagem, tipo = 'erro') => {
        const main = document.querySelector('main.pagina');
        if (!main) return;

        const aviso = document.createElement('div');

        aviso.className = `alerta-${tipo} notificacao-flutuante`;
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
    document.addEventListener('turbo:load', inicializar);

    window.addEventListener('resize', posicionarNotificacoes, {
        passive: true
    });
})();