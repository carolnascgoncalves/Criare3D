
const CHAVE_RASCUNHO = "rascunhoCriare3D";

function inicializarRascunhoCalculo() {
    const formulario = document.getElementById("formCalculo");

    if (document.getElementById("paginaResultado")) {
        sessionStorage.removeItem(CHAVE_RASCUNHO);
        return;
    }

    if (!formulario) return;

    const listaFilamentos = document.getElementById("listaFilamentos");
    const modeloFilamento = document.getElementById("modeloFilamento");

    function obterEtapaAtual() {
        const etapa = document.querySelector("[data-conteudo].ativa");
        return etapa ? Number(etapa.dataset.conteudo) : 1;
    }

    function salvarRascunho() {
        const campos = {};

        [...formulario.elements].forEach(campo => {
            if (!campo.name || campo.name.startsWith("filamentos[")) return;

            if (campo.type === "radio") {
                if (campo.checked) {
                    campos[campo.name] = campo.value;
                }
                return;
            }

            if (campo.type === "checkbox") {
                campos[campo.name] = campo.checked;
                return;
            }

            campos[campo.name] = campo.value;
        });

        const filamentos = listaFilamentos
            ? [...listaFilamentos.querySelectorAll("[data-filamento-item]")].map(item => ({
                filamentoId: item.querySelector("[data-campo-filamento]").value,
                quantidade: item.querySelector("[data-campo-quantidade]").value,
                confirmado: item.dataset.confirmado === "true"
            }))
            : [];

        const rascunho = {
            campos,
            filamentos,
            etapa: obterEtapaAtual()
        };

        sessionStorage.setItem(CHAVE_RASCUNHO, JSON.stringify(rascunho));
    }

    window.salvarRascunhoCalculo = salvarRascunho;

    function restaurarCampos(campos) {
        Object.entries(campos || {}).forEach(([nome, valor]) => {
            const campo = formulario.elements.namedItem(nome);

            if (!campo) return;

            if (campo instanceof RadioNodeList) {
                [...campo].forEach(opcao => {
                    opcao.checked = opcao.value === String(valor);
                });
                return;
            }

            if (campo.type === "checkbox") {
                campo.checked = Boolean(valor);
                return;
            }

            campo.value = valor ?? "";
        });

        formulario.querySelectorAll("select").forEach(campo => {
            campo.dispatchEvent(new Event("change", { bubbles: true }));
        });

        const tempoSalvo = campos?.tempoImpressaoMinutos;
        const tempo = document.getElementById("tempoImpressaoMinutos");

        if (tempo && tempoSalvo !== undefined) {
            tempo.value = tempoSalvo;
        }
    }

    function restaurarFilamentos(filamentos) {
        if (!listaFilamentos || !modeloFilamento || !Array.isArray(filamentos)) {
            return;
        }

        listaFilamentos.replaceChildren();

        filamentos.forEach((dados, indice) => {
            const item = modeloFilamento.content.firstElementChild.cloneNode(true);

            const select = item.querySelector("[data-campo-filamento]");
            const quantidade = item.querySelector("[data-campo-quantidade]");

            item.dataset.confirmado = dados.confirmado ? "true" : "false";
            item.hidden = dados.confirmado;

            select.name = `filamentos[${indice}].filamentoId`;
            select.id = `filamentoId${indice}`;
            select.value = dados.filamentoId || "";

            quantidade.name = `filamentos[${indice}].quantidadeFilamentoGramas`;
            quantidade.id = `quantidadeFilamentoGramas${indice}`;
            quantidade.value = dados.quantidade || "";

            listaFilamentos.appendChild(item);
        });

        listaFilamentos.querySelectorAll("[data-campo-filamento]").forEach(campo => {
            campo.dispatchEvent(new Event("change", { bubbles: true }));
        });

        listaFilamentos.querySelectorAll("[data-campo-quantidade]").forEach(campo => {
            campo.dispatchEvent(new Event("input", { bubbles: true }));
        });
    }

    function aplicarCadastrosRecentes() {
        const pecaCriada = document.getElementById("pecaCriada")?.value;
        const filamentoCriado = document.getElementById("filamentoCriado")?.value;
        const impressoraCriada = document.getElementById("impressoraCriada")?.value;
        const embalagemCriada = document.getElementById("embalagemCriada")?.value;

        if (pecaCriada) {
            const select = document.getElementById("pecaId");
            const tempo = document.getElementById("tempoImpressaoMinutos");
            const tempoSalvo = tempo?.value;

            if (select) {
                select.value = pecaCriada;
                select.dispatchEvent(new Event("change", { bubbles: true }));
            }

            if (tempo && tempoSalvo) {
                tempo.value = tempoSalvo;
            }
        }

        if (impressoraCriada) {
            const select = document.getElementById("impressoraId");

            if (select) {
                select.value = impressoraCriada;
            }
        }

        if (embalagemCriada) {
            const select = document.getElementById("embalagemId");

            if (select) {
                select.value = embalagemCriada;
            }
        }

        if (filamentoCriado && listaFilamentos) {
            const itens = [...listaFilamentos.querySelectorAll("[data-filamento-item]")];

            const item = itens.find(elemento =>
                !elemento.querySelector("[data-campo-filamento]").value
            ) || itens[itens.length - 1];

            if (item) {
                const select = item.querySelector("[data-campo-filamento]");

                select.value = filamentoCriado;
                item.hidden = false;
                item.dataset.confirmado = "false";

                select.dispatchEvent(new Event("change", { bubbles: true }));
            }
        }
    }

    const parametros = new URLSearchParams(window.location.search);

    // Mantém a possibilidade de iniciar um cálculo realmente novo.
    if (parametros.has("novo")) {
        sessionStorage.removeItem(CHAVE_RASCUNHO);
    }

    const rascunhoSalvo = sessionStorage.getItem(CHAVE_RASCUNHO);

    if (rascunhoSalvo) {
        try {
            const rascunho = JSON.parse(rascunhoSalvo);

            restaurarCampos(rascunho.campos);
            restaurarFilamentos(rascunho.filamentos);

            const fotoBase64 = document.getElementById("fotoPecaBase64")?.value;
            const fotoContentType = document.getElementById("fotoPecaContentType")?.value;

            if (fotoBase64 && fotoContentType) {
                const miniatura = document.getElementById("fotoPecaMiniatura");
                const preview = document.getElementById("fotoPecaPreview");
                const controle = document.getElementById("fotoPecaControle");

                if (miniatura) {
                    miniatura.src = `data:${fotoContentType};base64,${fotoBase64}`;
                }

                preview?.classList.add("visivel");
                controle?.classList.add("com-foto");
            }

            formulario.dispatchEvent(new CustomEvent("rascunhoCalculoRestaurado", {
                detail: { etapa: rascunho.etapa || 1 }
            }));
        } catch (erro) {
            console.error("Não foi possível restaurar o rascunho:", erro);
            sessionStorage.removeItem(CHAVE_RASCUNHO);
        }
    }

    aplicarCadastrosRecentes();

    formulario.addEventListener("input", salvarRascunho);
    formulario.addEventListener("change", salvarRascunho);
    formulario.addEventListener("submit", salvarRascunho);

    // Salva também quando a pessoa troca de etapa.
    formulario.addEventListener("click", event => {
        if (event.target.closest("[data-etapa], [data-proximo], [data-anterior]")) {
            requestAnimationFrame(salvarRascunho);
        }
    });

    // Salva antes de navegar pelo Turbo.
    document.addEventListener("turbo:before-visit", salvarRascunho);
    document.addEventListener("turbo:before-cache", salvarRascunho);

    // Também cobre navegações tradicionais do navegador.
    window.addEventListener("pagehide", salvarRascunho);

    salvarRascunho();
}

document.addEventListener("DOMContentLoaded", inicializarRascunhoCalculo);
document.addEventListener("turbo:load", inicializarRascunhoCalculo);
