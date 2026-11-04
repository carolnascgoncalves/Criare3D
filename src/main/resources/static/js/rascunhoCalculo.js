
(() => {
    const CHAVE_RASCUNHO = "rascunhoCriare3D";

    let formularioInicializado = null;

    function obterFormulario() {
        return document.getElementById("formCalculo");
    }

    function obterEtapaAtual() {
        const etapa = document.querySelector("[data-conteudo].ativa");
        return etapa ? Number(etapa.dataset.conteudo) : 1;
    }

    function salvarRascunho() {
        const formulario = obterFormulario();

        if (!formulario) return;

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

        const listaFilamentos = document.getElementById("listaFilamentos");

        const filamentos = listaFilamentos
            ? [...listaFilamentos.querySelectorAll("[data-filamento-item]")].map(item => ({
                filamentoId: item.querySelector("[data-campo-filamento]")?.value || "",
                quantidade: item.querySelector("[data-campo-quantidade]")?.value || "",
                confirmado: item.dataset.confirmado === "true"
            }))
            : [];

        sessionStorage.setItem(CHAVE_RASCUNHO, JSON.stringify({
            campos,
            filamentos,
            etapa: obterEtapaAtual()
        }));
    }

    window.salvarRascunhoCalculo = salvarRascunho;

    function restaurarCampos(campos, cadastroPecaNova) {
        const formulario = obterFormulario();

        Object.entries(campos || {}).forEach(([nome, valor]) => {
            // Uma peça recém-cadastrada deve substituir a seleção anterior.
            if (cadastroPecaNova && nome === "pecaId") return;

            // O tempo deve vir do cadastro da peça recém-criada.
            if (cadastroPecaNova && nome === "tempoImpressaoMinutos") return;

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

        // Os eventos de mudança podem preencher o tempo automaticamente.
        // Quando não é uma peça nova, mantemos o tempo que estava no rascunho.
        if (!cadastroPecaNova) {
            const tempo = document.getElementById("tempoImpressaoMinutos");

            if (tempo && campos?.tempoImpressaoMinutos !== undefined) {
                tempo.value = campos.tempoImpressaoMinutos;
                tempo.dispatchEvent(new Event("input", { bubbles: true }));
            }
        }
    }

    function restaurarFilamentos(filamentos) {
        const lista = document.getElementById("listaFilamentos");
        const modelo = document.getElementById("modeloFilamento");

        if (!lista || !modelo || !Array.isArray(filamentos)) return;

        lista.replaceChildren();

        filamentos.forEach((dados, indice) => {
            const item = modelo.content.firstElementChild.cloneNode(true);

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

            lista.appendChild(item);
        });

        lista.querySelectorAll("[data-campo-filamento]").forEach(campo => {
            campo.dispatchEvent(new Event("change", { bubbles: true }));
        });

        lista.querySelectorAll("[data-campo-quantidade]").forEach(campo => {
            campo.dispatchEvent(new Event("input", { bubbles: true }));
        });
    }

    function restaurarFoto() {
        const base64 = document.getElementById("fotoPecaBase64")?.value;
        const contentType = document.getElementById("fotoPecaContentType")?.value;

        if (!base64 || !contentType) return;

        const miniatura = document.getElementById("fotoPecaMiniatura");
        const preview = document.getElementById("fotoPecaPreview");
        const controle = document.getElementById("fotoPecaControle");

        if (miniatura) {
            miniatura.src = `data:${contentType};base64,${base64}`;
        }

        preview?.classList.add("visivel");
        controle?.classList.add("com-foto");
    }

    function aplicarCadastrosRecentes() {
        const pecaCriada = document.getElementById("pecaCriada")?.value;
        const filamentoCriado = document.getElementById("filamentoCriado")?.value;
        const impressoraCriada = document.getElementById("impressoraCriada")?.value;
        const embalagemCriada = document.getElementById("embalagemCriada")?.value;

        if (pecaCriada) {
            const select = document.getElementById("pecaId");

            if (select) {
                select.value = pecaCriada;
                select.dispatchEvent(new Event("change", { bubbles: true }));
            }
        }

        if (impressoraCriada) {
            const select = document.getElementById("impressoraId");

            if (select) {
                select.value = impressoraCriada;
                select.dispatchEvent(new Event("change", { bubbles: true }));
            }
        }

        if (embalagemCriada) {
            const select = document.getElementById("embalagemId");

            if (select) {
                select.value = embalagemCriada;
                select.dispatchEvent(new Event("change", { bubbles: true }));
            }
        }

        if (filamentoCriado) {
            const lista = document.getElementById("listaFilamentos");
            const modelo = document.getElementById("modeloFilamento");

            if (!lista || !modelo) return;

            let item = [...lista.querySelectorAll("[data-filamento-item]")]
                .find(elemento =>
                    !elemento.querySelector("[data-campo-filamento]").value &&
                    elemento.dataset.confirmado !== "true"
                );

            if (!item) {
                item = modelo.content.firstElementChild.cloneNode(true);
                lista.appendChild(item);
            }

            const select = item.querySelector("[data-campo-filamento]");

            item.hidden = false;
            item.dataset.confirmado = "false";
            select.value = filamentoCriado;

            select.dispatchEvent(new Event("change", { bubbles: true }));
        }
    }

    function configurarBotaoRefazer() {
        const botao = document.getElementById("refazerCalculo");

        if (!botao || botao.dataset.configurado === "true") return;

        botao.dataset.configurado = "true";

        botao.addEventListener("click", () => {
            const confirmar = window.confirm(
                "Tem certeza de que deseja refazer tudo?\n\nTodos os dados preenchidos neste cálculo serão apagados."
            );

            if (!confirmar) return;

            sessionStorage.removeItem(CHAVE_RASCUNHO);

            const destino = "/historico/novo?novo=true";

            if (window.Turbo) {
                window.Turbo.visit(destino);
            } else {
                window.location.href = destino;
            }
        });
    }

    function inicializarRascunhoCalculo() {
        // O resultado de um cálculo concluído não deve manter o rascunho.
        if (document.getElementById("paginaResultado")) {
            sessionStorage.removeItem(CHAVE_RASCUNHO);
            return;
        }

        const formulario = obterFormulario();

        if (!formulario || formulario === formularioInicializado) return;

        formularioInicializado = formulario;

        const parametros = new URLSearchParams(window.location.search);
        const cadastroPecaNova = parametros.has("pecaCriada");

        if (parametros.has("novo")) {
            sessionStorage.removeItem(CHAVE_RASCUNHO);
        }

        const salvo = sessionStorage.getItem(CHAVE_RASCUNHO);

        if (salvo) {
            try {
                const rascunho = JSON.parse(salvo);

                restaurarCampos(rascunho.campos, cadastroPecaNova);
                restaurarFilamentos(rascunho.filamentos);
                restaurarFoto();

                formulario.dispatchEvent(new CustomEvent("rascunhoCalculoRestaurado", {
                    detail: { etapa: rascunho.etapa || 1 }
                }));
            } catch (erro) {
                console.error("Não foi possível restaurar o rascunho:", erro);
                sessionStorage.removeItem(CHAVE_RASCUNHO);
            }
        }

        aplicarCadastrosRecentes();
        configurarBotaoRefazer();

        formulario.addEventListener("input", salvarRascunho);
        formulario.addEventListener("change", salvarRascunho);
        formulario.addEventListener("submit", salvarRascunho);

        formulario.addEventListener("click", event => {
            if (event.target.closest("[data-etapa], [data-proximo], [data-anterior]")) {
                requestAnimationFrame(salvarRascunho);
            }
        });

        salvarRascunho();
    }

    // Eventos globais registrados uma única vez.
    document.addEventListener("turbo:before-visit", salvarRascunho);
    document.addEventListener("turbo:before-cache", salvarRascunho);
    window.addEventListener("pagehide", salvarRascunho);

    document.addEventListener("DOMContentLoaded", inicializarRascunhoCalculo);
    document.addEventListener("turbo:load", inicializarRascunhoCalculo);
})();
