(() => {
    const chave = "criare3d:rolagem-apos-envio";

    function restaurar() {
        const salvo = sessionStorage.getItem(chave);
        if (!salvo) return;

        sessionStorage.removeItem(chave);
        try {
            const estado = JSON.parse(salvo);
            if (Date.now() - estado.criadoEm > 30000) return;
            requestAnimationFrame(() => window.scrollTo(0, estado.y));
        } catch {
            sessionStorage.removeItem(chave);
        }
    }

    document.addEventListener("submit", event => {
        const formulario = event.target;
        if (!(formulario instanceof HTMLFormElement) || formulario.id === "formCalculo") return;
        if ((formulario.method || "get").toLowerCase() !== "post") return;

        sessionStorage.setItem(chave, JSON.stringify({ y: window.scrollY, criadoEm: Date.now() }));
    }, true);

    document.addEventListener("DOMContentLoaded", restaurar);
    document.addEventListener("turbo:load", restaurar);
})();
