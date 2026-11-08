(function () {
    function prepararMenu() {
        const menu = document.querySelector('.menu-links-dropdown');
        if (!menu) return;
        const compacto = window.matchMedia('(max-width: 1100px)');
        menu.open = !compacto.matches;
        if (!menu.dataset.ajusteViewport) {
            compacto.addEventListener('change', evento => { menu.open = !evento.matches; });
            menu.dataset.ajusteViewport = 'true';
        }
    }
    document.addEventListener('DOMContentLoaded', prepararMenu);
    document.addEventListener('turbo:load', prepararMenu);
})();
