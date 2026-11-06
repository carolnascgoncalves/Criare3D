(() => {
    const chaveOrigem = "criare3d:origem-listagem";
    const chaveRolagemPost = "criare3d:rolagem-post";
    const chaveRolagemPaginacao = "criare3d:rolagem-paginacao";

    const validadePost = 30000;

    function salvarOrigem(url) {
        try {
            sessionStorage.setItem(chaveOrigem, JSON.stringify({
                url: url,
                y: window.scrollY
            }));
        } catch {
            // A navegação continua funcionando sem sessionStorage.
        }
    }

    function lerOrigem() {
        try {
            const salvo = sessionStorage.getItem(chaveOrigem);
            if (!salvo) return null;

            return JSON.parse(salvo);
        } catch {
            sessionStorage.removeItem(chaveOrigem);
            return null;
        }
    }

    function paginaEhDetalhes(caminho) {
        return caminho.includes("/detalhes/")
            || /^\/filamento\/[^/]+\/historico$/.test(caminho);
    }

    function atualizarLinksDeVolta() {
        const estado = lerOrigem();

        if (!estado || !estado.url) return;
        if (!paginaEhDetalhes(window.location.pathname)) return;

        document.querySelectorAll("main.pagina a[href]").forEach(link => {
            const destino = new URL(link.href, window.location.origin);

            const ehVoltar =
                destino.pathname.endsWith("/listar")
                || link.matches(
                    "a.botao-voltar, " +
                    ".pagina-intro a.botao-secundario, " +
                    ".formulario-acoes a.botao-secundario, " +
                    ".detalhes-acoes a"
                );

            if (ehVoltar) {
                link.href = estado.url;
            }
        });
    }

    function restaurarOrigem() {
        const estado = lerOrigem();
        if (!estado || !estado.url) return;

        const urlAtual = window.location.pathname + window.location.search;
        const urlSalva = new URL(estado.url, window.location.origin);
        const caminhoSalvo = urlSalva.pathname + urlSalva.search;

        if (urlAtual !== caminhoSalvo) return;

        sessionStorage.removeItem(chaveOrigem);

        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                window.scrollTo(0, Number(estado.y) || 0);
            });
        });
    }

    function salvarRolagemPost() {
        try {
            sessionStorage.setItem(chaveRolagemPost, JSON.stringify({
                y: window.scrollY,
                criadoEm: Date.now()
            }));
        } catch {
            // Não interrompe o envio do formulário.
        }
    }

    function restaurarRolagemPost() {
        try {
            const salvo = sessionStorage.getItem(chaveRolagemPost);
            if (!salvo) return;

            const estado = JSON.parse(salvo);

            if (!estado || Date.now() - estado.criadoEm > validadePost) {
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

    function salvarRolagemPaginacao(destino) {
        try {
            sessionStorage.setItem(chaveRolagemPaginacao, JSON.stringify({
                url: destino.pathname + destino.search,
                y: window.scrollY,
                criadoEm: Date.now()
            }));
        } catch {
            // A paginação continua funcionando sem sessionStorage.
        }
    }

    function restaurarRolagemPaginacao() {
        try {
            const salvo = sessionStorage.getItem(chaveRolagemPaginacao);
            if (!salvo) return;

            const estado = JSON.parse(salvo);

            if (!estado || Date.now() - estado.criadoEm > validadePost) {
                sessionStorage.removeItem(chaveRolagemPaginacao);
                return;
            }

            const urlAtual = window.location.pathname + window.location.search;

            if (urlAtual !== estado.url) return;

            sessionStorage.removeItem(chaveRolagemPaginacao);

            requestAnimationFrame(() => {
                requestAnimationFrame(() => {
                    window.scrollTo(0, Number(estado.y) || 0);
                });
            });
        } catch {
            sessionStorage.removeItem(chaveRolagemPaginacao);
        }
    }

    document.addEventListener("click", event => {
        if (!(event.target instanceof Element)) return;

        if (
            event.button !== 0
            || event.ctrlKey
            || event.metaKey
            || event.shiftKey
            || event.altKey
        ) {
            return;
        }

        const link = event.target.closest("a[href]");
        if (!link || !link.closest("main.pagina")) return;

        const destino = new URL(link.href, window.location.origin);

        if (destino.origin !== window.location.origin) return;

        if (link.closest(".paginacao-historico")) {
            salvarRolagemPaginacao(destino);
            return;
        }

        const caminho = destino.pathname;

        const abrindoDetalhes =
            caminho.includes("/detalhes/")
            || /^\/filamento\/[^/]+\/historico$/.test(caminho);

        if (abrindoDetalhes) {
            salvarOrigem(
                window.location.pathname + window.location.search
            );
        }
    }, true);

    document.addEventListener("submit", event => {
        const formulario = event.target;

        if (!(formulario instanceof HTMLFormElement)) return;
        if (formulario.id === "formCalculo") return;
        if ((formulario.method || "get").toLowerCase() !== "post") return;

        salvarRolagemPost();
    }, true);

    function inicializar() {
        document.querySelectorAll(".paginacao-historico a").forEach(link => {
            link.dataset.turbo = "false";
        });

        atualizarLinksDeVolta();
        restaurarOrigem();
        restaurarRolagemPost();
        restaurarRolagemPaginacao();
    }

    document.addEventListener("DOMContentLoaded", inicializar);
    document.addEventListener("turbo:load", inicializar);
    window.addEventListener("pageshow", restaurarRolagemPaginacao);
})();