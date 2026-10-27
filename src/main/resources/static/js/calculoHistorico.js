
document.addEventListener("DOMContentLoaded", () => {
    const formulario = document.getElementById("formCalculo");

    if (!formulario) {
        return;
    }

    const etapas = document.querySelectorAll("[data-etapa]");
    const conteudos = document.querySelectorAll("[data-conteudo]");
    const pecaSelect = document.getElementById("pecaId");
    const quantidadeInput = document.getElementById("quantidadeFilamentoGramas");
    const tempoInput = document.getElementById("tempoImpressaoMinutos");

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
        const conteudo = document.querySelector(
            `[data-conteudo="${etapaAtual}"]`
        );

        if (!conteudo) {
            return false;
        }

        const campos = conteudo.querySelectorAll(
            "input:not([type='hidden']), select, textarea"
        );

        for (const campo of campos) {
            if (campo.disabled || campo.type === "hidden") {
                continue;
            }

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
            return;
        }

        quantidadeInput.value = opcao.dataset.gramas || "";
        tempoInput.value = opcao.dataset.tempo || "";
    }

    pecaSelect.addEventListener("change", preencherDadosPeca);

    function configurarCadastroRapido(idSelect, rota) {
        const select = document.getElementById(idSelect);

        if (!select) {
            return;
        }

        select.addEventListener("change", () => {
            const opcao = select.selectedOptions[0];
            const acao = opcao?.dataset.acao;

            if (acao && acao.startsWith("cadastrar-")) {
                window.location.href = rota;
            }
        });
    }

    configurarCadastroRapido("pecaId", "/peca/novo");
    configurarCadastroRapido("filamentoId", "/filamento/novo");
    configurarCadastroRapido("impressoraId", "/impressora/novo");
    configurarCadastroRapido("embalagemId", "/embalagem/novo");

    formulario.addEventListener("submit", event => {
        if (!validarEtapa()) {
            event.preventDefault();
        }
    });

    mostrarEtapa(1);
});
