document.addEventListener("DOMContentLoaded", () => {
    const formulario = document.getElementById("formCalculo");

    if (!formulario) return;

    const etapas = document.querySelectorAll("[data-etapa]");
    const conteudos = document.querySelectorAll("[data-conteudo]");
    const pecaSelect = document.getElementById("pecaId");
    const tempoInput = document.getElementById("tempoImpressaoMinutos");
    const plataformaSelect = document.getElementById("plataformaVenda");
    const blocosPlataforma = document.querySelectorAll("[data-opcoes-plataforma]");

    let etapaAtual = 1;

    function mostrarEtapa(numero) {
        etapaAtual = numero;

        etapas.forEach(etapa => {
            const numeroEtapa = Number(etapa.dataset.etapa);

            etapa.classList.toggle("ativa", numeroEtapa === numero);
            etapa.classList.toggle("concluida", numeroEtapa < numero);
        });

        conteudos.forEach(conteudo => {
            conteudo.classList.toggle(
                "ativa",
                Number(conteudo.dataset.conteudo) === numero
            );
        });
    }

    function validarEtapa() {
        const conteudo = document.querySelector(`[data-conteudo="${etapaAtual}"]`);

        if (!conteudo) return false;

        const campos = conteudo.querySelectorAll("input:not([type='hidden']), select, textarea");

        for (const campo of campos) {
            if (campo.disabled || campo.type === "hidden" || campo.closest("[hidden]")) continue;

            if (!campo.checkValidity()) {
                campo.reportValidity();
                campo.focus();
                return false;
            }
        }

        return true;
    }

    document.querySelectorAll("[data-proximo]").forEach(botao => {
        botao.addEventListener("click", () => {
            if (validarEtapa() && etapaAtual < conteudos.length) {
                mostrarEtapa(etapaAtual + 1);
            }
        });
    });

    document.querySelectorAll("[data-anterior]").forEach(botao => {
        botao.addEventListener("click", () => {
            if (etapaAtual > 1) {
                mostrarEtapa(etapaAtual - 1);
            }
        });
    });

    function preencherDadosPeca() {
        const opcao = pecaSelect.selectedOptions[0];

        if (!opcao || !opcao.value || opcao.dataset.acao) {
            if (tempoInput) tempoInput.value = "";
            document.dispatchEvent(new Event("pecaSelecionada"));
            return;
        }

        if (tempoInput) {
            tempoInput.value = opcao.dataset.tempo || "";
        }

        document.dispatchEvent(new Event("pecaSelecionada"));
    }

    pecaSelect.addEventListener("change", preencherDadosPeca);

    function configurarCadastroRapido(idSelect, rota) {
        const select = document.getElementById(idSelect);

        if (!select) return;

        select.addEventListener("change", () => {
            const opcao = select.selectedOptions[0];

            if (opcao?.dataset.acao?.startsWith("cadastrar-")) {
                window.location.href = rota;
            }
        });
    }

    configurarCadastroRapido("pecaId", "/peca/novo");
    configurarCadastroRapido("impressoraId", "/impressora/novo");
    configurarCadastroRapido("embalagemId", "/embalagem/novo");

    function atualizarPlataforma() {
        if (!plataformaSelect) return;

        const selecionada = plataformaSelect.value;

        blocosPlataforma.forEach(bloco => {
            const ativo = bloco.dataset.opcoesPlataforma === selecionada;

            bloco.hidden = !ativo;

            bloco.querySelectorAll("input, select, textarea").forEach(campo => {
                campo.disabled = !ativo;
            });
        });
    }

    plataformaSelect?.addEventListener("change", atualizarPlataforma);
    atualizarPlataforma();

    formulario.addEventListener("submit", event => {
        const listaFilamentos = document.getElementById("listaFilamentos");
        const itensFilamento = [...listaFilamentos.querySelectorAll("[data-filamento-item]")];

        const pendente = itensFilamento.find(item => item.dataset.confirmado !== "true");

        if (pendente) {
            event.preventDefault();
            mostrarEtapa(1);
            pendente.hidden = false;

            const select = pendente.querySelector("[data-campo-filamento]");
            const quantidade = pendente.querySelector("[data-campo-quantidade]");

            requestAnimationFrame(() => {
                if (!select.value) {
                    select.reportValidity();
                    select.focus();
                } else if (!quantidade.checkValidity()) {
                    quantidade.reportValidity();
                    quantidade.focus();
                } else {
                    alert("Confirme o filamento antes de calcular os custos.");
                    pendente.querySelector("[data-confirmar-filamento]").focus();
                }
            });

            return;
        }

        const conteudosOrdenados = [...conteudos].sort(
            (a, b) => Number(a.dataset.conteudo) - Number(b.dataset.conteudo)
        );

        for (const conteudo of conteudosOrdenados) {
            const campos = conteudo.querySelectorAll("input:not([type='hidden']), select, textarea");

            for (const campo of campos) {
                if (campo.disabled || campo.type === "hidden" || campo.closest("[hidden]")) continue;

                if (!campo.checkValidity()) {
                    event.preventDefault();
                    mostrarEtapa(Number(conteudo.dataset.conteudo));

                    requestAnimationFrame(() => {
                        campo.reportValidity();
                        campo.focus();
                    });

                    return;
                }
            }
        }
    });

    // Começa pela primeira etapa, preservando os valores que o HTML renderizou.
    mostrarEtapa(1);
});