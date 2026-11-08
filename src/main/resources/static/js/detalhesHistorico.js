
(function () {

    document.addEventListener("click", event => {
        const botao = event.target.closest?.("[data-recalculo-bloqueado]");
        if (!botao) return;
        window.mostrarNotificacao?.(
            "Este cálculo já está salvo no histórico e não pode ser recalculado. Para usar os preços atuais, inicie um novo cálculo.",
            "info"
        );
    });

    function inicializarZoomFoto() {

        const botaoAbrir = document.getElementById("abrirFotoPeca");
        const modal = document.getElementById("modalFotoPeca");
        const botaoFechar = document.getElementById("fecharFotoPeca");

        if (!botaoAbrir || !modal || !botaoFechar) {
            return;
        }

        // Evita registrar os mesmos eventos duas vezes.
        if (botaoAbrir.dataset.zoomInicializado === "true") {
            return;
        }

        botaoAbrir.dataset.zoomInicializado = "true";

        function abrirModal() {
            modal.hidden = false;
            document.body.style.overflow = "hidden";
            botaoFechar.focus();
        }

        function fecharModal() {
            modal.hidden = true;
            document.body.style.overflow = "";
            botaoAbrir.focus();
        }

        botaoAbrir.addEventListener("click", abrirModal);

        botaoFechar.addEventListener("click", fecharModal);

        modal.addEventListener("click", function (event) {
            if (event.target === modal) {
                fecharModal();
            }
        });

        modal.addEventListener("keydown", function (event) {
            if (event.key === "Tab") {
                event.preventDefault();
                botaoFechar.focus();
            }
        });

        botaoAbrir.setAttribute("aria-haspopup", "dialog");
    }

    // Primeira abertura normal.
    document.addEventListener("DOMContentLoaded", inicializarZoomFoto);

    // Navegações internas realizadas pelo Turbo.
    document.addEventListener("turbo:load", inicializarZoomFoto);

    // Fecha o modal antes de o Turbo guardar a página em cache.
    document.addEventListener("turbo:before-cache", function () {

        const modal = document.getElementById("modalFotoPeca");

        if (modal) {
            modal.hidden = true;
        }

        document.body.style.overflow = "";
    });

    document.addEventListener("keydown", function (event) {

        if (event.key !== "Escape") {
            return;
        }

        const modal = document.getElementById("modalFotoPeca");

        if (modal && !modal.hidden) {
            modal.hidden = true;
            document.body.style.overflow = "";
            document.getElementById("abrirFotoPeca")?.focus();
        }
    });

})();
