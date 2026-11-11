let listaFilamentosInicializada = null;

function inicializarFilamentosCalculo() {
    const listaFilamentos = document.getElementById("listaFilamentos");
    const modeloFilamento = document.getElementById("modeloFilamento");
    const adicionarFilamento = document.getElementById("adicionarFilamento");
    const pecaId = document.getElementById("pecaId");
    const secaoFilamentos = document.getElementById("secaoFilamentos");
    const resumoConsumoPeca = document.getElementById("resumoConsumoPeca");
    const previaMateriaisPeca = document.getElementById("previaMateriaisPeca");
    const listaPreviaMateriais = document.querySelector("[data-previa-materiais-lista]");
    const statusPreviaMateriais = document.querySelector("[data-previa-materiais-status]");
    const aplicarMateriais = document.querySelector("[data-aplicar-materiais]");
    const nomePecaResumo = document.getElementById("nomePecaResumo");
    const consumoPrevistoResumo = document.getElementById("consumoPrevistoResumo");
    const listaResumo = document.getElementById("listaResumoConsumo");
    const totalFilamentoResumo = document.getElementById("totalFilamentoResumo");
    const totalPrevistoResumo = document.getElementById("totalPrevistoResumo");

    if (!listaFilamentos || !modeloFilamento || listaFilamentos === listaFilamentosInicializada) {return;}

    listaFilamentosInicializada = listaFilamentos;
    let materiaisSugeridos = [];
    let requisicaoMateriais = null;
    let ultimaPecaSelecionada = pecaId?.value || "";
    let processandoTrocaPeca = false;
    function formatarGramas(valor) {
        return `${Number(valor).toLocaleString("pt-BR", {
            maximumFractionDigits: 2
        })} g`;
    }

    function validarEstoque(item, avisar = false) {
        const select = item.querySelector("[data-campo-filamento]");
        const quantidade = item.querySelector("[data-campo-quantidade]");
        const opcao = select.selectedOptions[0];
        const estoque = Number(opcao?.dataset.estoque);

        if (!select.value || !Number.isFinite(estoque) || !quantidade.value) {
            quantidade.setCustomValidity("");
            item.dataset.ultimoAvisoEstoque = "";
            return true;
        }

        const consumoTotal = [...listaFilamentos.querySelectorAll("[data-filamento-item]")]
            .filter(outro => outro.querySelector("[data-campo-filamento]").value === select.value)
            .reduce((total, outro) => total + (Number(outro.querySelector("[data-campo-quantidade]").value) || 0), 0);

        if (consumoTotal > estoque + 0.000001) {
            const mensagem = `Estoque insuficiente de ${opcao.textContent.trim()}. Há ${formatarGramas(estoque)} disponíveis.`;
            quantidade.setCustomValidity(mensagem);
            if (avisar && item.dataset.ultimoAvisoEstoque !== mensagem) {
                window.mostrarNotificacao?.(mensagem, "erro");
                item.dataset.ultimoAvisoEstoque = mensagem;
            }
            return false;
        }

        quantidade.setCustomValidity("");
        item.dataset.ultimoAvisoEstoque = "";
        return true;
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
            item.dataset.confirmadoPeloUsuario ??= "false";

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

            const porcentagemIndividual = previsto > 0 ? filamento.gramas / previsto * 100 : 0;

            percentual.textContent = `${formatarPercentual(porcentagemIndividual)} do consumo previsto`;

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
                filamento.elemento.dataset.confirmadoPeloUsuario = "false";
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

        // Os campos são exibidos em gramas com até duas casas. Compare com a
        // mesma precisão para não classificar 147,35 como acima/abaixo por
        // resíduos binários da soma de números decimais no JavaScript.
        const totalComparavel = Math.round((totalAcumulado + Number.EPSILON) * 100) / 100;
        const previstoComparavel = Math.round((previsto + Number.EPSILON) * 100) / 100;
        const percentualTotal = previstoComparavel > 0 ? totalComparavel / previstoComparavel * 100 : 0;

        totalPrevistoResumo.textContent = `de ${formatarGramas(previsto)} previstos - ${formatarPercentual(percentualTotal)} do total`;

        const status = document.getElementById("statusConsumoResumo");

        if (status) {
            let classeStatus;
            let mensagem;

            if (totalComparavel < previstoComparavel) {
                classeStatus = "consumo-abaixo";
                mensagem = "Quantidade abaixo do previsto";
            } else if (totalComparavel > previstoComparavel) {
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

    function atualizarDisponibilidadeFilamentos() {
        if (secaoFilamentos) {
            secaoFilamentos.hidden = !pecaId?.value;
        }
    }

    function existemFilamentosConfirmados() {
        return [...listaFilamentos.querySelectorAll("[data-filamento-item]")]
            .some(item => item.dataset.confirmadoPeloUsuario === "true");
    }

    async function trocarPeca() {
        if (processandoTrocaPeca) return;
        const novaPeca = pecaId.value;
        if (novaPeca === ultimaPecaSelecionada) return;

        processandoTrocaPeca = true;
        pecaId.disabled = true;
        try {
            if (existemFilamentosConfirmados()) {
                const mensagem = "Ao trocar a peça, os filamentos confirmados serão apagados. Deseja continuar?";
                const confirmar = window.confirmarAcao
                    ? await window.confirmarAcao(mensagem, pecaId)
                    : window.confirm(mensagem);

                if (!confirmar) {
                    pecaId.value = ultimaPecaSelecionada;
                    pecaId.dispatchEvent(new Event("change", {bubbles: true}));
                    atualizarDisponibilidadeFilamentos();
                    atualizarTudo();
                    pecaId.disabled = false;
                    window.salvarRascunhoCalculo?.();
                    return;
                }
            }

            listaFilamentos.replaceChildren();
            criarFilamento();
            ultimaPecaSelecionada = novaPeca;
            atualizarDisponibilidadeFilamentos();
            atualizarTudo();
            pecaId.disabled = false;
            window.salvarRascunhoCalculo?.();
        } finally {
            pecaId.disabled = false;
            processandoTrocaPeca = false;
        }

        await carregarMateriaisDaPeca();
    }

    function formatarCor(cor) {
        if (!cor || !/^#(?:[\da-f]{3}|[\da-f]{6})$/i.test(cor)) return "";
        return cor.length === 4
            ? `#${cor[1]}${cor[1]}${cor[2]}${cor[2]}${cor[3]}${cor[3]}`.toLowerCase()
            : cor.toLowerCase();
    }

    function exibirMateriaisSugeridos(materiais) {
        if (!previaMateriaisPeca || !listaPreviaMateriais || !statusPreviaMateriais) return;
        listaPreviaMateriais.replaceChildren();
        materiaisSugeridos = materiais.filter(item => Number(item.pesoGramas) > 0);
        if (materiaisSugeridos.length === 0) {
            previaMateriaisPeca.hidden = true;
            return;
        }

        materiaisSugeridos.forEach(item => {
            const linha = document.createElement("div");
            linha.className = "previa-material-item";
            const identificacao = document.createElement("div");
            identificacao.className = "previa-material-identificacao";
            const amostra = document.createElement("span");
            amostra.className = "previa-material-cor";
            const cor = formatarCor(item.corHex);
            if (cor) {
                amostra.style.backgroundColor = cor;
                amostra.title = item.corHex;
                amostra.setAttribute("aria-label", `Cor ${item.corHex}`);
            } else {
                amostra.classList.add("sem-cor");
                amostra.setAttribute("aria-label", "Cor não informada");
            }
            const texto = document.createElement("div");
            const nome = document.createElement("strong");
            nome.textContent = item.material || "Material sem identificação";
            const detalhes = document.createElement("small");
            detalhes.textContent = cor ? item.corHex : "Cor não identificada no arquivo";
            texto.append(nome, detalhes);
            identificacao.append(amostra, texto);
            const peso = document.createElement("strong");
            peso.className = "previa-material-peso";
            peso.textContent = `${Number(item.pesoGramas).toLocaleString("pt-BR", {minimumFractionDigits: 2, maximumFractionDigits: 2})} g`;
            linha.append(identificacao, peso);
            listaPreviaMateriais.appendChild(linha);
        });

        statusPreviaMateriais.textContent = `${materiaisSugeridos.length} ${materiaisSugeridos.length === 1 ? "filamento encontrado" : "filamentos encontrados"}. Você pode usar as gramagens e escolher seus rolos cadastrados.`;
        aplicarMateriais.disabled = false;
        previaMateriaisPeca.hidden = false;
    }

    async function carregarMateriaisDaPeca() {
        if (!previaMateriaisPeca || !pecaId?.value
            || pecaId.selectedOptions[0]?.dataset.temArquivo !== "true") {
            if (previaMateriaisPeca) previaMateriaisPeca.hidden = true;
            requisicaoMateriais?.abort();
            materiaisSugeridos = [];
            return;
        }
        requisicaoMateriais?.abort();
        requisicaoMateriais = new AbortController();
        const controller = requisicaoMateriais;
        previaMateriaisPeca.hidden = true;
        statusPreviaMateriais.textContent = "Verificando os dados do arquivo...";
        aplicarMateriais.disabled = true;
        try {
            const resposta = await fetch(`/peca/${encodeURIComponent(pecaId.value)}/materiais-calculo`, {
                headers: {Accept: "application/json"},
                credentials: "same-origin",
                signal: controller.signal
            });
            if (!resposta.ok || controller !== requisicaoMateriais) return;
            exibirMateriaisSugeridos(await resposta.json());
        } catch (erro) {
            if (erro.name !== "AbortError") previaMateriaisPeca.hidden = true;
        }
    }

    function encontrarFilamentoCorrespondente(select, material) {
        const corAlvo = formatarCor(material.corHex);
        const nomeMaterial = (material.material || "").toLocaleLowerCase("pt-BR");
        const tiposConhecidos = ["PLA", "PETG", "ABS", "ASA", "TPU", "PVA", "HIPS", "PC"];
        const tipoAlvo = tiposConhecidos.find(tipo => nomeMaterial.includes(tipo.toLocaleLowerCase("pt-BR")))
            ?.toLocaleLowerCase("pt-BR");
        if (!corAlvo || (!tipoAlvo && !nomeMaterial)) return null;
        const opcoes = [...select.options].filter(opcao => {
            if (!opcao.value || formatarCor(opcao.dataset.codigoCor) !== corAlvo) return false;
            const nome = opcao.textContent.toLocaleLowerCase("pt-BR");
            return tipoAlvo ? nome.includes(tipoAlvo) : nome.includes(nomeMaterial);
        });
        return opcoes.length === 1 ? opcoes[0] : null;
    }

    function atualizarSugestaoFilamento(item) {
        const indicacao = item.querySelector("[data-sugestao-filamento]");
        const texto = item.querySelector("[data-sugestao-filamento-texto]");
        const amostra = item.querySelector(".filamento-sugestao-cor");
        if (!indicacao || !texto || !amostra || !item.dataset.materialArquivo) return;

        const corArquivo = formatarCor(item.dataset.corArquivo);
        const select = item.querySelector("[data-campo-filamento]");
        const materialArquivo = item.dataset.materialArquivo;
        const corDescricao = item.dataset.corArquivo || "não identificada";
        amostra.style.backgroundColor = corArquivo || "transparent";

        const recomendacao = encontrarFilamentoCorrespondente(select, {
            material: materialArquivo,
            corHex: item.dataset.corArquivo
        });
        const opcoesMesmaCor = [...select.options].filter(opcao => opcao.value
            && corArquivo && formatarCor(opcao.dataset.codigoCor) === corArquivo);
        const opcaoRecomendada = recomendacao || (opcoesMesmaCor.length === 1 ? opcoesMesmaCor[0] : null);

        if (opcaoRecomendada) {
            texto.textContent = `Recomendação do arquivo: ${materialArquivo} · ${corDescricao}. Filamento correspondente no cadastro: ${opcaoRecomendada.textContent.trim()}.`;
        } else {
            texto.textContent = `Recomendação do arquivo: ${materialArquivo} · ${corDescricao}.`;
        }
        indicacao.hidden = false;
    }

    aplicarMateriais?.addEventListener("click", async () => {
        if (!materiaisSugeridos.length) return;
        const preenchidos = [...listaFilamentos.querySelectorAll("[data-filamento-item]")]
            .some(item => item.dataset.confirmado === "true"
                || item.querySelector("[data-campo-filamento]")?.value
                || item.querySelector("[data-campo-quantidade]")?.value);
        if (preenchidos) {
            const confirmar = window.confirmarAcao
                ? await window.confirmarAcao("Substituir os filamentos atuais pelas sugestões do arquivo?", aplicarMateriais)
                : window.confirm("Substituir os filamentos atuais pelas sugestões do arquivo?");
            if (!confirmar) return;
        }

        listaFilamentos.replaceChildren();
        const idsUsados = new Set();
        let estoqueInsuficiente = false;
        materiaisSugeridos.forEach(material => {
            const item = modeloFilamento.content.firstElementChild.cloneNode(true);
            const select = item.querySelector("[data-campo-filamento]");
            const quantidade = item.querySelector("[data-campo-quantidade]");
            item.hidden = false;
            item.dataset.confirmado = "false";
            item.dataset.confirmadoPeloUsuario = "false";
            item.dataset.materialArquivo = material.material || "Material";
            item.dataset.corArquivo = material.corHex || "";
            quantidade.value = Number(material.pesoGramas).toFixed(2);

            const correspondencia = encontrarFilamentoCorrespondente(select, material);
            if (correspondencia && !idsUsados.has(correspondencia.value)) {
                select.value = correspondencia.value;
                idsUsados.add(correspondencia.value);
            }
            listaFilamentos.appendChild(item);
            atualizarSugestaoFilamento(item);
            if (select.value && validarEstoque(item, false)) {
                item.dataset.confirmado = "true";
                item.dataset.confirmadoPeloUsuario = "false";
                item.hidden = true;
            } else if (select.value) {
                estoqueInsuficiente = true;
            }
        });
        atualizarTudo();
        window.salvarRascunhoCalculo?.();
        const pendentes = [...listaFilamentos.querySelectorAll("[data-filamento-item]")]
            .filter(item => item.dataset.confirmado !== "true").length;
        window.mostrarNotificacao?.(
            estoqueInsuficiente
                ? "As gramagens foram preenchidas, mas há filamento com estoque insuficiente. Confira os rolos e quantidades."
                : pendentes
                    ? `Gramagens preenchidas. Confira os rolos para ${pendentes} ${pendentes === 1 ? "material" : "materiais"}.`
                    : "Filamentos preenchidos com os dados do arquivo. Confira as quantidades antes de avançar.",
            estoqueInsuficiente ? "erro" : "sucesso"
        );
        if (pendentes) {
            const primeiroSemRolo = [...listaFilamentos.querySelectorAll("[data-filamento-item]")]
                .find(item => item.dataset.confirmado !== "true");
            primeiroSemRolo.hidden = false;
            primeiroSemRolo.querySelector("[data-campo-filamento]").focus();
        }
    });

    function criarFilamento() {
        const novo = modeloFilamento.content.firstElementChild.cloneNode(true);

        novo.dataset.confirmado = "false";
        novo.dataset.confirmadoPeloUsuario = "false";
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

            if (!validarEstoque(item, true)) {
                quantidade.focus();
                window.validacaoInline?.validar(quantidade);
                return;
            }

            if (!select.checkValidity()) {
                window.validacaoInline?.validar(select);
                select.focus();
                return;
            }

            if (!quantidade.checkValidity()) {
                window.validacaoInline?.validar(quantidade);
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
                window.mostrarNotificacao?.(
                    "Esse filamento já foi confirmado. Edite o filamento existente para alterar a quantidade.",
                    "erro"
                );
                return;
            }

            item.dataset.confirmado = "true";
            item.dataset.confirmadoPeloUsuario = "true";
            item.hidden = true;
            atualizarTudo();
        }

        if (remover) {
            const itens = listaFilamentos.querySelectorAll("[data-filamento-item]");

            if (itens.length === 1) {
                const item = itens[0];
                item.dataset.confirmado = "false";
                item.dataset.confirmadoPeloUsuario = "false";
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
        if (select) {
            const opcao = select.selectedOptions[0];

            if (opcao?.dataset.acao === "cadastrar-filamento") {
                window.salvarRascunhoCalculo?.();

                const destino = "/filamento/novo?origem=calculo";

                if (window.Turbo) {
                    window.Turbo.visit(destino);
                } else {
                    window.location.href = destino;
                }

                return;
            }

            validarEstoque(select.closest("[data-filamento-item]"), true);
            atualizarTudo();
            return;
        }

        const quantidade = event.target.closest("[data-campo-quantidade]");
        if (quantidade) {
            validarEstoque(quantidade.closest("[data-filamento-item]"), true);
            atualizarResumo();
        }
    });

    listaFilamentos.addEventListener("input", event => {
        if (event.target.matches("[data-campo-quantidade]")) {
            validarEstoque(event.target.closest("[data-filamento-item]"), true);
            atualizarResumo();
        }
    });

    document.getElementById("formCalculo")?.addEventListener("submit", event => {
        const itens = [...listaFilamentos.querySelectorAll("[data-filamento-item]")];
        const invalido = itens.find(item => !validarEstoque(item, true));
        if (!invalido) return;
        event.preventDefault();
        invalido.hidden = false;
        invalido.dataset.confirmado = "false";
        invalido.dataset.confirmadoPeloUsuario = "false";
        invalido.querySelector("[data-campo-quantidade]").focus();
        atualizarTudo();
    });

    pecaId?.addEventListener("change", trocarPeca, true);
    pecaId?.addEventListener("pecaSelecionada", () => {
        if (processandoTrocaPeca) return;
        atualizarDisponibilidadeFilamentos();
        atualizarTudo();
        carregarMateriaisDaPeca();
    });

    atualizarDisponibilidadeFilamentos();
    atualizarTudo();
    carregarMateriaisDaPeca();
}

document.addEventListener("DOMContentLoaded", inicializarFilamentosCalculo);
document.addEventListener("turbo:load", inicializarFilamentosCalculo);
