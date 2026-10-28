document.addEventListener("DOMContentLoaded", () => {
    const listaFilamentos = document.getElementById("listaFilamentos");
    const modeloFilamento = document.getElementById("modeloFilamento");
    const adicionarFilamento = document.getElementById("adicionarFilamento");
    const pecaId = document.getElementById("pecaId");
    const resumoConsumoPeca = document.getElementById("resumoConsumoPeca");
    const nomePecaResumo = document.getElementById("nomePecaResumo");
    const consumoPrevistoResumo = document.getElementById("consumoPrevistoResumo");
    const listaResumo = document.getElementById("listaResumoConsumo");
    const totalFilamentoResumo = document.getElementById("totalFilamentoResumo");
    const totalPrevistoResumo = document.getElementById("totalPrevistoResumo");
    const formulario = document.getElementById("formCalculo");

    if (!listaFilamentos || !modeloFilamento) return;

    function formatarGramas(valor) {
        return `${Number(valor).toLocaleString("pt-BR", {
            maximumFractionDigits: 2
        })} g`;
    }

    function obterPrevisto() {
        const opcao = pecaId?.selectedOptions[0];
        return Number(opcao?.dataset.gramas) || 0;
    }

    function atualizarNomesCampos() {
        const itens = listaFilamentos.querySelectorAll("[data-filamento-item]");

        itens.forEach((item, indice) => {
            const select = item.querySelector("[data-campo-filamento]");
            const quantidade = item.querySelector("[data-campo-quantidade]");
            const titulo = item.querySelector(".filamento-calculo-cabecalho strong");
            const remover = item.querySelector("[data-remover-filamento]");

            item.dataset.confirmado ??= "false";

            select.name = `filamentos[${indice}].filamentoId`;
            select.id = `filamentoId${indice}`;

            quantidade.name = `filamentos[${indice}].quantidadeFilamentoGramas`;
            quantidade.id = `quantidadeFilamentoGramas${indice}`;

            if (titulo) titulo.textContent = `Filamento ${indice + 1}`;
            if (remover) remover.hidden = itens.length === 1;
        });
    }

    function atualizarResumo() {
        const previsto = obterPrevisto();
        const pecaSelecionada = pecaId?.selectedOptions[0];
        const itens = [...listaFilamentos.querySelectorAll("[data-filamento-item]")];
        let totalAcumulado = 0;

        listaResumo.innerHTML = "";

        if (resumoConsumoPeca) {
            resumoConsumoPeca.hidden = !pecaId?.value;

            if (pecaSelecionada && pecaId.value) {
                nomePecaResumo.textContent = pecaSelecionada.textContent.trim();
                consumoPrevistoResumo.textContent =
                    `Consumo previsto: ${formatarGramas(previsto)}`;
            }
        }

        itens.forEach((item) => {
            if (item.dataset.confirmado !== "true") return;

            const select = item.querySelector("[data-campo-filamento]");
            const quantidade = item.querySelector("[data-campo-quantidade]");
            const opcao = select.selectedOptions[0];

            const gramas = Number(quantidade.value) || 0;
            totalAcumulado += gramas;

            const cartao = document.createElement("div");
            cartao.className = "resumo-consumo-filamento";

            const cabecalho = document.createElement("div");
            cabecalho.className = "resumo-consumo-filamento-cabecalho";

            const identificacao = document.createElement("div");
            identificacao.className = "resumo-consumo-identificacao";

            const codigoCor = opcao.dataset.codigoCor;
            const nomeCor = opcao.dataset.cor;
            const amostra = document.createElement("span");
            amostra.className = "amostra-cor";

            if (codigoCor) {
                amostra.style.backgroundColor = codigoCor;
            }

            const nome = document.createElement("strong");
            nome.textContent = nomeCor
                ? `${opcao.textContent.trim()} — ${nomeCor}`
                : opcao.textContent.trim();

            const quantidadeTexto = document.createElement("span");
            quantidadeTexto.className = "resumo-consumo-quantidade";
            quantidadeTexto.textContent = `${formatarGramas(gramas)} utilizados`;

            const textoIdentificacao = document.createElement("div");
            textoIdentificacao.className = "resumo-consumo-identificacao-texto";
            textoIdentificacao.append(nome, quantidadeTexto);

            identificacao.append(amostra, textoIdentificacao);
            cabecalho.appendChild(identificacao);

            const medidor = document.createElement("div");
            medidor.className = "resumo-consumo-medidor";

            const topoMedidor = document.createElement("div");
            topoMedidor.className = "resumo-consumo-medidor-topo";

            const porcentagem = previsto > 0
                ? (totalAcumulado / previsto) * 100
                : 0;

            const porcentagemVisivel = Math.min(porcentagem, 100);

            const valor = document.createElement("strong");
            valor.className = "resumo-consumo-medidor-valor";
            valor.textContent =
                `${formatarGramas(totalAcumulado)}/${formatarGramas(previsto)}`;

            const barra = document.createElement("div");
            barra.className = "resumo-consumo-barra";

            const progresso = document.createElement("div");
            progresso.className = "resumo-consumo-progresso";
            progresso.style.width = `${porcentagemVisivel}%`;

            if (porcentagem > 100) {
                progresso.classList.add("acima-do-previsto");
            }

            barra.appendChild(progresso);

            const percentual = document.createElement("p");
            percentual.className = "resumo-consumo-percentual";
            percentual.textContent =
                `${Number(porcentagem.toFixed(1)).toLocaleString("pt-BR")}% do previsto`;

            topoMedidor.appendChild(valor);
            medidor.append(topoMedidor, barra, percentual);

            const acoes = document.createElement("div");
            acoes.className = "resumo-consumo-acoes";

            const editar = document.createElement("button");
            editar.type = "button";
            editar.className = "botao botao-secundario";
            editar.innerHTML = '<i class="bi bi-pencil"></i> Editar';
            editar.addEventListener("click", () => {
                item.dataset.confirmado = "false";
                item.hidden = false;
                atualizarTudo();
            });

            const excluir = document.createElement("button");
            excluir.type = "button";
            excluir.className = "botao-remover-filamento";
            excluir.innerHTML = '<i class="bi bi-trash"></i> Excluir';
            excluir.addEventListener("click", () => {
                item.remove();

                if (!listaFilamentos.querySelector("[data-filamento-item]")) {
                    criarFilamento();
                }

                atualizarTudo();
            });

            acoes.append(editar, excluir);
            cartao.append(cabecalho, medidor, acoes);
            listaResumo.appendChild(cartao);

            item.hidden = true;
        });

        totalFilamentoResumo.textContent = formatarGramas(totalAcumulado);
        totalPrevistoResumo.textContent = `de ${formatarGramas(previsto)} previstos`;

        const status = document.getElementById("statusConsumoResumo");

        if (status) {
            status.textContent = totalAcumulado <= previsto
                ? "Quantidade dentro do previsto"
                : "Quantidade acima do previsto";
        }
    }

    function atualizarTudo() {
        atualizarNomesCampos();
        atualizarResumo();
    }

    function criarFilamento() {
        const novo = modeloFilamento.content.firstElementChild.cloneNode(true);

        novo.dataset.confirmado = "false";
        novo.hidden = false;

        novo.querySelector("[data-campo-filamento]").value = "";
        novo.querySelector("[data-campo-quantidade]").value = "";

        listaFilamentos.appendChild(novo);
        atualizarTudo();
    }

    adicionarFilamento?.addEventListener("click", () => {
        criarFilamento();

        const itens = listaFilamentos.querySelectorAll("[data-filamento-item]");
        const novo = itens[itens.length - 1];

        novo.querySelector("[data-campo-filamento]").focus();
    });

    listaFilamentos.addEventListener("click", (event) => {
        const confirmar = event.target.closest("[data-confirmar-filamento]");
        const remover = event.target.closest("[data-remover-filamento]");

        if (confirmar) {
            const item = confirmar.closest("[data-filamento-item]");
            const select = item.querySelector("[data-campo-filamento]");
            const quantidade = item.querySelector("[data-campo-quantidade]");

            if (!select.checkValidity()) {
                select.reportValidity();
                select.focus();
                return;
            }

            if (!quantidade.checkValidity()) {
                quantidade.reportValidity();
                quantidade.focus();
                return;
            }

            const duplicado = [...listaFilamentos.querySelectorAll("[data-filamento-item]")]
                .some(outro =>
                    outro !== item &&
                    outro.dataset.confirmado === "true" &&
                    outro.querySelector("[data-campo-filamento]").value === select.value
                );

            if (duplicado) {
                alert("Esse filamento já foi confirmado. Edite o filamento existente para alterar a quantidade.");
                return;
            }

            item.dataset.confirmado = "true";
            item.hidden = true;
            atualizarTudo();
        }

        if (remover) {
            const itens = listaFilamentos.querySelectorAll("[data-filamento-item]");

            if (itens.length === 1) {
                const item = itens[0];
                item.dataset.confirmado = "false";
                item.hidden = false;
                item.querySelector("[data-campo-filamento]").value = "";
                item.querySelector("[data-campo-quantidade]").value = "";
            } else {
                remover.closest("[data-filamento-item]").remove();
            }

            atualizarTudo();
        }
    });

    listaFilamentos.addEventListener("change", (event) => {
        const select = event.target.closest("[data-campo-filamento]");

        if (!select) return;

        const opcao = select.selectedOptions[0];

        if (opcao?.dataset.acao === "cadastrar-filamento") {
            window.location.href = "/filamento/novo";
            return;
        }

        atualizarResumo();
    });

    listaFilamentos.addEventListener("input", (event) => {
        if (event.target.matches("[data-campo-quantidade]")) {
            atualizarResumo();
        }
    });

    pecaId?.addEventListener("change", atualizarTudo);
    document.addEventListener("pecaSelecionada", atualizarTudo);

    atualizarTudo();
});