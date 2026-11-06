(() => {
    const chave = "criare3d:rolagem-navegacao";
    const validade = 30000;

    function salvar() {
        try {
            sessionStorage.setItem(chave, JSON.stringify({
                y: window.scrollY,
                criadoEm: Date.now()
            }));
        } catch {
            // A navegação continua funcionando mesmo se o armazenamento estiver indisponível.
        }
    }

    function restaurar() {
        let salvo;

        try {
            salvo = sessionStorage.getItem(chave);
            if (!salvo) return;

            sessionStorage.removeItem(chave);

            const estado = JSON.parse(salvo);

            if (!estado || Date.now() - estado.criadoEm > validade) return;

            requestAnimationFrame(() => {
                window.scrollTo(0, estado.y);
            });
        } catch {
            sessionStorage.removeItem(chave);
        }
    }

    document.addEventListener("submit", event => {
        const formulario = event.target;

        if (!(formulario instanceof HTMLFormElement)) return;
        if (formulario.id === "formCalculo") return;
        if ((formulario.method || "get").toLowerCase() !== "post") return;

        salvar();
    }, true);

    document.addEventListener("turbo:before-visit", salvar);
    document.addEventListener("DOMContentLoaded", restaurar);
    document.addEventListener("turbo:load", restaurar);
})();