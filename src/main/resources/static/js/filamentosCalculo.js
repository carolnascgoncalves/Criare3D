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

    if (!listaFilamentos || !modeloFilamento) return;

    function formatarGramas(valor) {
        return `${Number(valor).toLocaleString("pt-BR", {
            maximumFractionDigits: 2
        })} g`;
    }

    function formatarPercentual(valor) {
        return `${Number(valor.toFixed(1)).toLocaleString("pt-BR")}%`;
    }

    function obterPrevisto() {
        const opcao = pecaId?.selectedOptions[0];
        return Number(opcao?.dataset.gramas) || 0;
    }

    function obterCodigoCor(codigo) {
        if (codigo && /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(codigo.trim())) {
            return codigo.trim();
        }

        return "#E9A4C7";
    }

    function clarearCor(hex, proporcao = 0.55) {
        const cor = obterCodigoCor(hex).replace("#", "");

        const valor = cor.length === 3
            ? cor.split("").map(letra => letra + letra).join("")
            : cor;

        const vermelho = parseInt(valor.substring(0, 2), 16);
        const verde = parseInt(valor.substring(2, 4), 16);
        const azul = parseInt(valor.substring(4, 6), 16);

        const clarear = componente =>
            Math.round(componente + (255 - componente) * proporcao);

        return `rgb(${clarear(vermelho)}, ${clarear(verde)}, ${clarear(azul)})`;
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

    function criarSegmento(filamento, largura, anterior) {
        const segmento = document.createElement("div");

        segmento.style.width = `${largura}%`;
        segmento.style.height = "100%";
        segmento.style.flex = `0 0 ${largura}%`;
        segmento.style.backgroundColor = anterior
            ? clarearCor(filamento.codigoCor)
            : obterCodigoCor(filamento.codigoCor);
        segmento.style.boxSizing = "border-box";

        return segmento;
    }

    function atualizarResumo() {
        const previsto = obterPrevisto();
        const pecaSelecionada = pecaId?.selectedOptions[0];
        const itens = [...listaFilamentos.querySelectorAll("[data-filamento-item]")];

        listaResumo.innerHTML = "";

        if (resumoConsumoPeca) {
            resumoConsumoPeca.hidden = !pecaId?.value;

            if (pecaSelecionada && pecaId.value) {
                nomePecaResumo.textContent = pecaSelecionada.textContent.trim();
                consumoPrevistoResumo.textContent = `Consumo previsto: ${formatarGramas(previsto)}`;
            }
        }

        const confirmados = itens
            .filter(item => item.dataset.confirmado === "true")
            .map(item => {
                const select = item.querySelector("[data-campo-filamento]");
                const quantidade = item.querySelector("[data-campo-quantidade]");
                const opcao = select.selectedOptions[0];

                return {
                    elemento: item,
                    gramas: Number(quantidade.value) || 0,
                    nome: opcao?.textContent.trim() || "Filamento",
                    codigoCor: obterCodigoCor(opcao?.dataset.codigoCor)
                };
            });

        const totalAcumulado = confirmados.reduce(
            (total, item) => total + item.gramas,
            0
        );

        confirmados.forEach((filamento, indice) => {
            const cartao = document.createElement("div");
            cartao.className = "resumo-consumo-filamento";

            const consumoAcumulado = confirmados
                .slice(0, indice + 1)
                .reduce((total, item) => total + item.gramas, 0);

            const cabecalho = document.createElement("div");
            cabecalho.className = "resumo-consumo-filamento-cabecalho";

            const identificacao = document.createElement("div");
            identificacao.className = "resumo-consumo-identificacao";

            const amostra = document.createElement("span");
            amostra.className = "amostra-cor";
            amostra.style.backgroundColor = filamento.codigoCor;

            const nome = document.createElement("strong");
            nome.textContent = filamento.nome;

            const quantidadeTexto = document.createElement("span");
            quantidadeTexto.className = "resumo-consumo-quantidade";
            quantidadeTexto.textContent = `${formatarGramas(filamento.gramas)} neste filamento`;

            const textoIdentificacao = document.createElement("div");
            textoIdentificacao.className = "resumo-consumo-identificacao-texto";
            textoIdentificacao.append(nome, quantidadeTexto);

            identificacao.append(amostra, textoIdentificacao);
            cabecalho.appendChild(identificacao);

            const medidor = document.createElement("div");
            medidor.className = "resumo-consumo-medidor";

            const topoMedidor = document.createElement("div");
            topoMedidor.className = "resumo-consumo-medidor-topo";

            const valor = document.createElement("strong");
            valor.className = "resumo-consumo-medidor-valor";
            valor.textContent = `${formatarGramas(consumoAcumulado)} / ${formatarGramas(previsto)}`;

            const barra = document.createElement("div");
            barra.className = "resumo-consumo-barra";
            barra.style.display = "flex";
            barra.style.overflow = "hidden";
            barra.style.position = "relative";

            let larguraUsada = 0;

            const segmentos = [
                filamento,
                ...confirmados.slice(0, indice).reverse()
            ];

            segmentos.forEach((segmento, posicao) => {
                if (previsto <= 0 || larguraUsada >= 100) return;

                const larguraOriginal = segmento.gramas / previsto * 100;
                const larguraDisponivel = 100 - larguraUsada;
                const largura = Math.min(larguraOriginal, larguraDisponivel);

                if (largura <= 0) return;

                barra.appendChild(
                    criarSegmento(segmento, largura, posicao > 0)
                );

                larguraUsada += largura;
            });

            const percentual = document.createElement("p");
            percentual.className = "resumo-consumo-percentual";

            const porcentagemIndividual = totalAcumulado > 0
                ? filamento.gramas / totalAcumulado * 100
                : 0;

            percentual.textContent =
                `${formatarPercentual(porcentagemIndividual)} do total de filamentos`;

            topoMedidor.appendChild(valor);
            medidor.append(topoMedidor, barra, percentual);

            const acoes = document.createElement("div");
            acoes.className = "resumo-consumo-acoes";

            const editar = document.createElement("button");
            editar.type = "button";
            editar.className = "botao botao-secundario";
            editar.innerHTML = '<i class="bi bi-pencil"></i> Editar';

            editar.addEventListener("click", () => {
                filamento.elemento.dataset.confirmado = "false";
                filamento.elemento.hidden = false;
                atualizarTudo();
                filamento.elemento.querySelector("[data-campo-filamento]").focus();
            });

            const excluir = document.createElement("button");
            excluir.type = "button";
            excluir.className = "botao-remover-filamento";
            excluir.innerHTML = '<i class="bi bi-trash"></i> Excluir';

            excluir.addEventListener("click", () => {
                filamento.elemento.remove();

                if (!listaFilamentos.querySelector("[data-filamento-item]")) {
                    criarFilamento();
                }

                atualizarTudo();
            });

            acoes.append(editar, excluir);
            cartao.append(cabecalho, medidor, acoes);
            listaResumo.appendChild(cartao);

            filamento.elemento.hidden = true;
        });

        totalFilamentoResumo.textContent = formatarGramas(totalAcumulado);

        const percentualTotal = totalAcumulado > 0
            ? previsto / totalAcumulado * 100
            : 0;

        totalPrevistoResumo.textContent =
            `de ${formatarGramas(previsto)} previstos - ${formatarPercentual(percentualTotal)} do total`;

        const status = document.getElementById("statusConsumoResumo");

        if (status) {
            let classeStatus;
            let mensagem;

            if (totalAcumulado < previsto) {
                classeStatus = "consumo-abaixo";
                mensagem = "Quantidade abaixo do previsto";
            } else if (totalAcumulado > previsto) {
                classeStatus = "consumo-acima";
                mensagem = "Quantidade acima do previsto";
            } else {
                classeStatus = "consumo-dentro";
                mensagem = "Quantidade dentro do previsto";
            }

            status.textContent = mensagem;
            status.classList.remove("consumo-dentro", "consumo-acima", "consumo-abaixo");
            status.classList.add(classeStatus);
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
        itens[itens.length - 1].querySelector("[data-campo-filamento]").focus();
    });

    listaFilamentos.addEventListener("click", event => {
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

    listaFilamentos.addEventListener("change", event => {
        const select = event.target.closest("[data-campo-filamento]");

        if (!select) return;

        const opcao = select.selectedOptions[0];

        if (opcao?.dataset.acao === "cadastrar-filamento") {
            window.location.href = "/filamento/novo";
            return;
        }

        atualizarResumo();
    });

    listaFilamentos.addEventListener("input", event => {
        if (event.target.matches("[data-campo-quantidade]")) {
            atualizarResumo();
        }
    });

    pecaId?.addEventListener("change", atualizarTudo);
    document.addEventListener("pecaSelecionada", atualizarTudo);

    atualizarTudo();
});