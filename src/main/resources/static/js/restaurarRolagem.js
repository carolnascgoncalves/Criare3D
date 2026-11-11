
(() => {
    const CHAVE_ROLAGEM = "criare3d:rolagem-navegacao";
    const CHAVE_ORIGEM = "criare3d:origem-listagem";
    const VALIDADE = 20000;

    function areaRolagem() {
        return document.querySelector("[data-rolagem-pagina]");
    }

    function posicaoRolagem() {
        return areaRolagem()?.scrollTop ?? window.scrollY;
    }

    function caminho(url) {
        const destino = new URL(url, window.location.origin);
        return destino.pathname + destino.search;
    }

    function salvarRolagem(destino = null, y = posicaoRolagem()) {
        try {
            sessionStorage.setItem(CHAVE_ROLAGEM, JSON.stringify({
                destino: destino ? caminho(destino) : null,
                y,
                criadoEm: Date.now()
            }));
        } catch {
            // A navegação continua funcionando sem sessionStorage.
        }
    }

    function lerEstado(chave) {
        try {
            const valor = sessionStorage.getItem(chave);
            return valor ? JSON.parse(valor) : null;
        } catch {
            sessionStorage.removeItem(chave);
            return null;
        }
    }

    function salvarOrigemDetalhes(url) {
        try {
            sessionStorage.setItem(CHAVE_ORIGEM, JSON.stringify({
                url: caminho(url),
                y: posicaoRolagem()
            }));
        } catch {
            // Não interrompe a navegação.
        }
    }

    function ehDetalhes(caminhoAtual) {
        return caminhoAtual.includes("/detalhes/")
            || /^\/filamento\/[^/]+\/historico$/.test(caminhoAtual);
    }

    function origemValidaDoHistorico(url) {
        try {
            return new URL(url, window.location.origin).pathname === "/historico/listar";
        } catch {
            return false;
        }
    }

    function atualizarLinksVoltar() {
        const origem = lerEstado(CHAVE_ORIGEM);

        if (!origem || !origem.url) return;
        if (!ehDetalhes(window.location.pathname)) return;

        // Em detalhes de cálculo, o retorno deve vir da listagem. Ao abrir
        // detalhes a partir do orçamento, não transforme o link em orçamento.
        if (window.location.pathname.startsWith("/historico/detalhes/")
            && !origemValidaDoHistorico(origem.url)) {
            sessionStorage.removeItem(CHAVE_ORIGEM);
            return;
        }

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
                link.href = origem.url;
            }
        });
    }

    function restaurarRolagem() {
        let estado = lerEstado(CHAVE_ROLAGEM);

        if (estado && Date.now() - estado.criadoEm > VALIDADE) {
            sessionStorage.removeItem(CHAVE_ROLAGEM);
            estado = null;
        }

        if (estado?.destino && estado.destino !== caminho(window.location.href)) {
            return;
        }

        if (!estado) {
            const origem = lerEstado(CHAVE_ORIGEM);

            if (origem && origem.url === caminho(window.location.href)) {
                estado = {
                    y: origem.y
                };

                sessionStorage.removeItem(CHAVE_ORIGEM);
            }
        }

        if (!estado) return;

        sessionStorage.removeItem(CHAVE_ROLAGEM);

        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                (areaRolagem() || window).scrollTo({
                    top: Number(estado.y) || 0,
                    left: 0,
                    behavior: "instant"
                });

                const origem = lerEstado(CHAVE_ORIGEM);

                if (origem && origem.url === caminho(window.location.href)) {
                    sessionStorage.removeItem(CHAVE_ORIGEM);
                }
            });
        });
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
        if (link.target && link.target !== "_self") return;
        if (link.hasAttribute("download")) return;

        const destino = new URL(link.href, window.location.origin);

        if (destino.origin !== window.location.origin) return;

        if (
            destino.pathname === window.location.pathname
            && destino.search === window.location.search
            && destino.hash
        ) {
            return;
        }

        const abrindoDetalhes =
            destino.pathname.includes("/detalhes/")
            || /^\/filamento\/[^/]+\/historico$/.test(destino.pathname);

        if (abrindoDetalhes && window.location.pathname === "/historico/listar") {
            salvarOrigemDetalhes(window.location.href);
        }

        let posicao = posicaoRolagem();
        const origem = lerEstado(CHAVE_ORIGEM);

        if (origem && origem.url === caminho(destino)) {
            posicao = origem.y;
        }

        salvarRolagem(destino, posicao);
    }, true);

    document.addEventListener("submit", event => {
        const formulario = event.target;

        if (!(formulario instanceof HTMLFormElement)) return;
        if (formulario.id === "formCalculo") return;

        const acao = new URL(
            formulario.action || window.location.href,
            window.location.origin
        );

        if (acao.origin !== window.location.origin) return;

        salvarRolagem();
    }, true);

    document.addEventListener("turbo:before-visit", event => {
        const destino = event.detail?.url;

        if (destino) {
            const origem = lerEstado(CHAVE_ORIGEM);
            let posicao = posicaoRolagem();

            if (origem && origem.url === caminho(destino)) {
                posicao = origem.y;
            }

            salvarRolagem(destino, posicao);
        }
    });

    function inicializar() {
        atualizarLinksVoltar();
        restaurarRolagem();
    }

    document.addEventListener("DOMContentLoaded", inicializar);
    document.addEventListener("turbo:load", inicializar);
    document.addEventListener("turbo:frame-load", restaurarRolagem);
    window.addEventListener("pageshow", restaurarRolagem);
})();
