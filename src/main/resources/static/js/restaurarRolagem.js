(() => {
    const chaveOrigem = "criare3d:origem-listagem";
    const chaveRolagemPost = "criare3d:rolagem-post";
    const validade = 30000;

    function salvarOrigem(url) {
        try {
            sessionStorage.setItem(chaveOrigem, JSON.stringify({
                url,
                y: window.scrollY,
                criadoEm: Date.now()
            }));
        } catch {
            // A navegação continua funcionando mesmo sem sessionStorage.
        }
    }

    function lerOrigem() {
        try {
            const salvo = sessionStorage.getItem(chaveOrigem);
            if (!salvo) return null;

            const estado = JSON.parse(salvo);

            if (!estado || Date.now() - estado.criadoEm > validade) {
                sessionStorage.removeItem(chaveOrigem);
                return null;
            }

            return estado;
        } catch {
            sessionStorage.removeItem(chaveOrigem);
            return null;
        }
    }

    function restaurarOrigem() {
        const estado = lerOrigem();
        if (!estado) return;

        const urlAtual = window.location.pathname + window.location.search;
        const urlSalva = new URL(estado.url, window.location.origin);
        const caminhoSalvo = urlSalva.pathname + urlSalva.search;

        if (urlAtual !== caminhoSalvo) {
            return;
        }

        sessionStorage.removeItem(chaveOrigem);

        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                window.scrollTo(0, estado.y);
            });
        });
    }

    function atualizarLinkDeVolta() {
        const estado = lerOrigem();
        if (!estado) return;

        const caminhoAtual = window.location.pathname;

        const paginaDeDetalhes =
            caminhoAtual.includes("/detalhes/") ||
            caminhoAtual.endsWith("/historico");

        if (!paginaDeDetalhes) return;

        const linkVoltar = document.querySelector(
            "main.pagina .pagina-intro a.botao-secundario"
        );

        if (linkVoltar) {
            linkVoltar.href = estado.url;
        }
    }

    document.addEventListener("click", event => {
        const link = event.target.closest("a[href]");
        if (!link || !link.closest("main.pagina")) return;

        const destino = new URL(link.href, window.location.origin);

        if (destino.origin !== window.location.origin) return;

        const caminho = destino.pathname;

        const abrindoDetalhes =
            caminho.includes("/detalhes/") ||
            (caminho.includes("/filamento/") && caminho.endsWith("/historico"));

        if (abrindoDetalhes) {
            salvarOrigem(window.location.pathname + window.location.search);
        }
    }, true);

    document.addEventListener("submit", event => {
        const formulario = event.target;

        if (!(formulario instanceof HTMLFormElement)) return;
        if (formulario.id === "formCalculo") return;
        if ((formulario.method || "get").toLowerCase() !== "post") return;

        try {
            sessionStorage.setItem(chaveRolagemPost, JSON.stringify({
                y: window.scrollY,
                criadoEm: Date.now()
            }));
        } catch {
            // Não interrompe o envio do formulário.
        }
    }, true);

    function restaurarPost() {
        try {
            const salvo = sessionStorage.getItem(chaveRolagemPost);
            if (!salvo) return;

            const estado = JSON.parse(salvo);

            if (!estado || Date.now() - estado.criadoEm > validade) {
                sessionStorage.removeItem(chaveRolagemPost);
                return;
            }

            sessionStorage.removeItem(chaveRolagemPost);

            requestAnimationFrame(() => {
                requestAnimationFrame(() => {
                    window.scrollTo(0, estado.y);
                });
            });
        } catch {
            sessionStorage.removeItem(chaveRolagemPost);
        }
    }

    function inicializar() {
        atualizarLinkDeVolta();
        restaurarOrigem();
        restaurarPost();
    }

    document.addEventListener("DOMContentLoaded", inicializar);
    document.addEventListener("turbo:load", inicializar);
})();