(() => {
    const CHAVE = 'criare3d-menu-recolhido';
    const consultaMobile = window.matchMedia('(max-width: 760px)');

    function aplicarEstado() {
        const menu = document.querySelector('[data-menu-alternar]');
        if (!menu) return;
        const mobile = consultaMobile.matches;
        const recolhido = mobile || localStorage.getItem(CHAVE) === 'true';
        document.body.classList.toggle('menu-lateral-recolhido', recolhido && !mobile);
        document.body.classList.remove('menu-lateral-aberto');
        menu.setAttribute('aria-expanded', String(!recolhido));
        menu.setAttribute('aria-label', recolhido ? 'Expandir menu' : 'Recolher menu');
        menu.title = recolhido ? 'Expandir menu' : 'Recolher menu';
        menu.innerHTML = `<i class="bi ${recolhido ? 'bi-chevron-right' : 'bi-chevron-left'}" aria-hidden="true"></i>`;
    }

    function iniciar() {
        const menu = document.querySelector('[data-menu-alternar]');
        if (!menu || menu.dataset.inicializado === 'true') return;
        menu.dataset.inicializado = 'true';
        aplicarEstado();
        menu.addEventListener('click', () => {
            if (consultaMobile.matches) {
                const aberto = document.body.classList.toggle('menu-lateral-aberto');
                menu.setAttribute('aria-expanded', String(aberto));
                menu.setAttribute('aria-label', aberto ? 'Fechar menu' : 'Abrir menu');
                menu.title = aberto ? 'Fechar menu' : 'Abrir menu';
                menu.innerHTML = `<i class="bi ${aberto ? 'bi-x-lg' : 'bi-list'}" aria-hidden="true"></i>`;
                return;
            }
            const recolhido = document.body.classList.toggle('menu-lateral-recolhido');
            localStorage.setItem(CHAVE, String(recolhido));
            menu.setAttribute('aria-expanded', String(!recolhido));
            menu.setAttribute('aria-label', recolhido ? 'Expandir menu' : 'Recolher menu');
            menu.title = recolhido ? 'Expandir menu' : 'Recolher menu';
            menu.innerHTML = `<i class="bi ${recolhido ? 'bi-chevron-right' : 'bi-chevron-left'}" aria-hidden="true"></i>`;
        });
        document.querySelectorAll('.menu-lateral-link, .menu-calcular').forEach(link =>
            link.addEventListener('click', () => { if (consultaMobile.matches) aplicarEstado(); }));
        if (!document.documentElement.dataset.menuResizeListener) {
            consultaMobile.addEventListener('change', aplicarEstado);
            document.documentElement.dataset.menuResizeListener = 'true';
        }
    }

    document.addEventListener('DOMContentLoaded', iniciar);
    document.addEventListener('turbo:load', iniciar);
})();
