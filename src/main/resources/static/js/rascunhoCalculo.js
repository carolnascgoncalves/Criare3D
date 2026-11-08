
(() => {
    const CHAVE_RASCUNHO = "rascunhoCriare3D";

    let formularioInicializado = null;
    let logoutEmAndamento = false;

    function obterFormulario() {
        return document.getElementById("formCalculo");
    }

    function obterEtapaAtual() {
        const etapa = document.querySelector("[data-conteudo].ativa");
        return etapa ? Number(etapa.dataset.conteudo) : 1;
    }

    function salvarRascunho() {
        if (logoutEmAndamento) return;

        const formulario = obterFormulario();

        if (!formulario) return;

        const campos = {};

        [...formulario.elements].forEach(campo => {
            if (
                !campo.name ||
                campo.name.startsWith("filamentos[") ||
                campo.type === "file" ||
                campo.type === "submit" ||
                campo.type === "button"
            ) {
                return;
            }

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
            if (cadastroPecaNova && (
                nome === "pecaId" ||
                nome === "tempoImpressaoMinutos"
            )) {
                return;
            }

            const campo = formulario.elements.namedItem(nome);

            if (!campo || campo.type === "file") return;

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

        // Compatibilidade com rascunhos antigos que guardavam o número de fornadas.
        if (campos?.pecasPorImpressao === undefined && campos?.unidades !== undefined) {
            const capacidade = formulario.elements.namedItem("pecasPorImpressao");
            if (capacidade) {
                const unidades = Math.max(1, Number(campos.unidades) || 1);
                const impressoesAntigas = Math.max(1, Number(campos.fornadas) || unidades);
                capacidade.value = String(Math.max(1, Math.ceil(unidades / impressoesAntigas)));
            }
        }

        formulario.querySelectorAll("select").forEach(campo => {
            campo.dispatchEvent(new Event("change", { bubbles: true }));
        });

        if (!cadastroPecaNova) {
            const tempo = document.getElementById("tempoImpressaoMinutos");

            if (tempo && campos?.tempoImpressaoMinutos !== undefined) {
                tempo.value = campos.tempoImpressaoMinutos;
                tempo.dataset.tempoManual = "true";
                tempo.dispatchEvent(new Event("input", { bubbles: true }));
            }
        }
    }

    function restaurarFilamentos(filamentos) {
        const lista = document.getElementById("listaFilamentos");
        const modelo = document.getElementById("modeloFilamento");

        if (!lista || !modelo || !Array.isArray(filamentos)) return;

        lista.replaceChildren();

        const dadosFilamentos = filamentos.length > 0
            ? filamentos
            : [{ filamentoId: "", quantidade: "", confirmado: false }];

        dadosFilamentos.forEach((dados, indice) => {
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
        // Arquivos de foto não são guardados no rascunho local; o servidor mantém a foto
        // temporária durante o fluxo calcular -> resultado -> salvar.
    }

    function aplicarCadastrosRecentes() {
        const pecaCriada = document.getElementById("pecaCriada")?.value;
        const filamentoCriado = document.getElementById("filamentoCriado")?.value;
        const impressoraCriada = document.getElementById("impressoraCriada")?.value;
        const embalagemCriada = document.getElementById("embalagemCriada")?.value;

        if (pecaCriada) {
            const select = document.getElementById("pecaId");
            const tempo = document.getElementById("tempoImpressaoMinutos");

            if (select) {
                select.value = pecaCriada;

                const opcao = select.selectedOptions[0];

                if (tempo) {
                    tempo.value = opcao?.dataset.tempo || "";
                    tempo.dispatchEvent(new Event("input", { bubbles: true }));
                }

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

                item.dataset.confirmado = "false";
                item.hidden = false;

                const indice = lista.querySelectorAll("[data-filamento-item]").length;

                const select = item.querySelector("[data-campo-filamento]");
                const quantidade = item.querySelector("[data-campo-quantidade]");

                select.name = `filamentos[${indice}].filamentoId`;
                select.id = `filamentoId${indice}`;

                quantidade.name = `filamentos[${indice}].quantidadeFilamentoGramas`;
                quantidade.id = `quantidadeFilamentoGramas${indice}`;

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
        document.addEventListener("click", event => {
            const botao = event.target.closest?.("[data-refazer-calculo]");

            if (!botao) return;

            event.preventDefault();

            const confirmar = window.confirm(
                "Tem certeza de que deseja refazer tudo?\n\n" +
                "Todos os dados preenchidos neste cálculo serão apagados."
            );

            if (!confirmar) return;

            sessionStorage.removeItem(CHAVE_RASCUNHO);

            window.location.assign("/historico/novo?novo=true");
        });
    }

    function inicializarRascunhoCalculo() {
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

        // Ao reabrir um cálculo salvo para recalcular com preços atuais, os dados
        // carregados pelo servidor devem prevalecer sobre um rascunho anterior.
        if (parametros.has("editar")) {
            sessionStorage.removeItem(CHAVE_RASCUNHO);
        }

        const respostaCalculoComErro =
            document.getElementById("respostaCalculoComErro")?.value === "true";

        // Em uma resposta de erro, os valores do servidor são os mais recentes.
        // Não substituí-los por um rascunho antigo do sessionStorage.
        if (respostaCalculoComErro) {
            sessionStorage.removeItem(CHAVE_RASCUNHO);
        }

        const salvo = respostaCalculoComErro || parametros.has("editar")
            ? null
            : sessionStorage.getItem(CHAVE_RASCUNHO);

        if (salvo) {
            try {
                const rascunho = JSON.parse(salvo);

                restaurarCampos(rascunho.campos, cadastroPecaNova);
                restaurarFilamentos(rascunho.filamentos);
                restaurarFoto();

                aplicarCadastrosRecentes();

                formulario.dispatchEvent(new CustomEvent("rascunhoCalculoRestaurado", {
                    detail: { etapa: rascunho.etapa || 1 }
                }));

            } catch (erro) {
                console.error("Não foi possível restaurar o rascunho:", erro);
                sessionStorage.removeItem(CHAVE_RASCUNHO);
            }
        } else {
            aplicarCadastrosRecentes();
        }

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

    // Salva antes de navegar, inclusive para os cadastros rápidos.
    document.addEventListener("submit", event => {
        if (event.target?.id !== "logoutForm") return;

        logoutEmAndamento = true;
        sessionStorage.removeItem(CHAVE_RASCUNHO);
    }, true);

    document.addEventListener("turbo:before-visit", salvarRascunho);
    document.addEventListener("turbo:before-cache", salvarRascunho);
    window.addEventListener("pagehide", salvarRascunho);

    configurarBotaoRefazer();

    document.addEventListener("DOMContentLoaded", inicializarRascunhoCalculo);
    document.addEventListener("turbo:load", inicializarRascunhoCalculo);
})();
