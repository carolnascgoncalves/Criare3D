(() => {
    function inicializarMenuConta() {
        const menu = document.querySelector(".menu-usuario");
        const painel = menu?.querySelector(".menu-usuario-painel");
        const dialogo = document.querySelector(".confirmacao-logout");
        if (!menu || !painel || !dialogo || menu.dataset.inicializado === "true") return;
        menu.dataset.inicializado = "true";

        const ajustarPosicao = () => {
            if (!menu.open || window.innerWidth > 600) return;
            const menuPrincipal = document.querySelector(".menu-principal");
            const topo = menuPrincipal ? menuPrincipal.getBoundingClientRect().bottom + 8 : 88;
            painel.style.setProperty("--menu-conta-topo", `${Math.max(8, topo)}px`);
        };

        menu.addEventListener("toggle", ajustarPosicao);
        window.addEventListener("resize", ajustarPosicao);

        menu.addEventListener("click", (event) => {
            if (!event.target.closest("[data-abrir-confirmacao-logout]")) return;
            event.preventDefault();
            dialogo.showModal();
        });

        dialogo.addEventListener("click", (event) => {
            if (event.target === dialogo || event.target.closest("[data-cancelar-logout]")) {
                dialogo.close();
                return;
            }
            if (!event.target.closest("[data-confirmar-logout]")) return;
            const form = menu.querySelector("[data-logout-form]");
            if (form) form.requestSubmit();
            dialogo.close();
        });
    }

    document.addEventListener("DOMContentLoaded", inicializarMenuConta);
    document.addEventListener("turbo:load", inicializarMenuConta);
})();
