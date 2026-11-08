
let formularioEtapasInicializado = null;

function inicializarCalculoEtapas() {
    const formulario = document.getElementById("formCalculo");

    if (!formulario || formulario === formularioEtapasInicializado) return;

    formularioEtapasInicializado = formulario;

    const etapas = document.querySelectorAll("[data-etapa]");
    const conteudos = document.querySelectorAll("[data-conteudo]");
    const pecaSelect = document.getElementById("pecaId");
    const tempoInput = document.getElementById("tempoImpressaoMinutos");
    const unidadesInput = document.getElementById("unidades");
    const pecasPorImpressaoInput = document.getElementById("pecasPorImpressao");
    const impressoraSelect = document.getElementById("impressoraId");
    const embalagemSelect = document.getElementById("embalagemId");
    const listaFilamentos = document.getElementById("listaFilamentos");
    const plataformaSelect = document.getElementById("plataformaVenda");
    const blocosPlataforma = document.querySelectorAll("[data-opcoes-plataforma]");

    function numeroImpressoes() {
        const unidades = Math.max(1, Number(unidadesInput?.value) || 1);
        const capacidade = Math.max(1, Number(pecasPorImpressaoInput?.value) || 1);
        return Math.ceil(unidades / capacidade);
    }

    function atualizarResumoProducao(atualizarTempo = false) {
        const unidades = Math.max(1, Number(unidadesInput?.value) || 1);
        const capacidade = Math.max(1, Number(pecasPorImpressaoInput?.value) || 1);
        const impressoes = numeroImpressoes();
        const resumo = document.getElementById("resumoImpressoes");
        if (resumo) {
            const tamanhos = Array.from({length: impressoes}, (_, i) =>
                Math.min(capacidade, Math.max(0, unidades - i * capacidade)));
            resumo.textContent = `${unidades} ${unidades === 1 ? "unidade" : "unidades"} → ${impressoes} ${impressoes === 1 ? "impressão" : "impressões"} (${tamanhos.join(" + ")})`;
        }
        const previsto = Number(pecaSelect?.selectedOptions[0]?.dataset.tempo) || 0;
        if (atualizarTempo && tempoInput && tempoInput.dataset.tempoManual !== "true" && previsto > 0) {
            tempoInput.value = String(previsto * Math.min(capacidade, unidades));
        }
        const uni = document.getElementById("previaUnidades");
        const imp = document.getElementById("previaImpressoes");
        const temp = document.getElementById("previaTempo");
        if (uni) uni.textContent = `${unidades} ${unidades === 1 ? "unidade" : "unidades"}`;
        if (imp) imp.textContent = `${impressoes} ${impressoes === 1 ? "impressão" : "impressões"}`;
        if (temp) temp.textContent = `${(Number(tempoInput?.value) || 0) * impressoes} min de máquina`;
        const material = document.getElementById("previaMaterial");
        if (material) {
            const gramas = [...(listaFilamentos?.querySelectorAll("[data-filamento-item]") || [])]
                .filter(item => item.dataset.confirmado === "true")
                .reduce((total, item) => total + (Number(item.querySelector("[data-campo-quantidade]")?.value) || 0), 0)
                * unidades;
            material.textContent = `${new Intl.NumberFormat("pt-BR", {maximumFractionDigits: 1}).format(gramas)} g${gramas > 0 ? " + purga estimada" : ""}`;
        }
        atualizarResumoTempo();
    }

    unidadesInput?.addEventListener("input", () => {
        atualizarResumoProducao(true);
        salvarRascunho();
    });
    pecasPorImpressaoInput?.addEventListener("input", () => {
        atualizarResumoProducao(true);
        salvarRascunho();
    });

    let etapaAtual = 1;

    function salvarRascunho() {
        window.salvarRascunhoCalculo?.();
    }

    function mostrarEtapa(numero, salvar = true) {
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

        if (salvar) {
            salvarRascunho();
        }
        if (numero >= 3) {
            clearTimeout(temporizadorPrevia);
            temporizadorPrevia = setTimeout(atualizarPreviaCustos, 150);
        }
    }

    function focarCampo(campo) {
        if (!campo) return;

        requestAnimationFrame(() => {
            window.validacaoInline?.validar(campo);
            campo.focus();
        });
    }

    function validarCamposDaEtapa(numero) {
        const conteudo = document.querySelector(
            `[data-conteudo="${numero}"]`
        );

        if (!conteudo) return false;

        const campos = conteudo.querySelectorAll(
            "input:not([type='hidden']), select, textarea"
        );

        for (const campo of campos) {
            if (
                campo.disabled ||
                campo.type === "hidden" ||
                campo.closest("[hidden]")
            ) {
                continue;
            }

            if (!campo.checkValidity()) {
                focarCampo(campo);
                salvarRascunho();
                return false;
            }
        }

        return true;
    }

    function validarFilamentos() {
        if (!listaFilamentos) return false;

        const itens = [
            ...listaFilamentos.querySelectorAll("[data-filamento-item]")
        ];

        if (itens.length === 0) {
            window.mostrarNotificacao?.("Adicione pelo menos um filamento.", "erro");
            return false;
        }

        let existeConfirmado = false;

        for (const item of itens) {
            if (item.dataset.confirmado === "true") {
                existeConfirmado = true;
                continue;
            }

            item.hidden = false;

            const select = item.querySelector("[data-campo-filamento]");
            const quantidade = item.querySelector("[data-campo-quantidade]");

            if (!select.value || !select.checkValidity()) {
                focarCampo(select);
                salvarRascunho();
                return false;
            }

            if (!quantidade.checkValidity()) {
                focarCampo(quantidade);
                salvarRascunho();
                return false;
            }

            window.mostrarNotificacao?.("Confirme todos os filamentos antes de avançar.", "erro");
            item.querySelector("[data-confirmar-filamento]")?.focus();

            salvarRascunho();
            return false;
        }

        if (!existeConfirmado) {
            const primeiroItem = itens[0];

            primeiroItem.hidden = false;

            const select = primeiroItem.querySelector("[data-campo-filamento]");
            const quantidade = primeiroItem.querySelector("[data-campo-quantidade]");

            if (!select.value) {
                focarCampo(select);
            } else if (!quantidade.checkValidity()) {
                focarCampo(quantidade);
            } else {
                window.mostrarNotificacao?.("Confirme pelo menos um filamento antes de avançar.", "erro");
                primeiroItem.querySelector("[data-confirmar-filamento]")?.focus();
            }

            salvarRascunho();
            return false;
        }

        return true;
    }

    function validarEtapa(numero) {
        if (numero === 1) {
            if (!pecaSelect?.checkValidity()) {
                focarCampo(pecaSelect);
                salvarRascunho();
                return false;
            }

            if (unidadesInput && !unidadesInput.checkValidity()) {
                focarCampo(unidadesInput);
                salvarRascunho();
                return false;
            }

            return validarFilamentos();
        }

        if (numero === 2) {
            if (!impressoraSelect?.checkValidity()) {
                focarCampo(impressoraSelect);
                salvarRascunho();
                return false;
            }

            if (!tempoInput?.checkValidity()) {
                focarCampo(tempoInput);
                salvarRascunho();
                return false;
            }

            if (pecasPorImpressaoInput && !pecasPorImpressaoInput.checkValidity()) {
                focarCampo(pecasPorImpressaoInput);
                salvarRascunho();
                return false;
            }

            return true;
        }

        if (numero === 3) {
            // Quando a embalagem é fixa, o campo não existe e não precisa ser validado.
            if (embalagemSelect && !embalagemSelect.checkValidity()) {
                focarCampo(embalagemSelect);
                salvarRascunho();
                return false;
            }

            return true;
        }

        if (numero === 4) {
            return validarCamposDaEtapa(4);
        }

        return true;
    }

    function navegarParaEtapa(destino) {
        if (destino < 1 || destino > conteudos.length) return;

        // Voltar para uma etapa anterior nunca exige validação.
        if (destino <= etapaAtual) {
            mostrarEtapa(destino);
            return;
        }

        // Para avançar, todas as etapas anteriores precisam estar válidas.
        for (let numero = 1; numero < destino; numero++) {
            if (!validarEtapa(numero)) {
                mostrarEtapa(numero);
                return;
            }
        }

        mostrarEtapa(destino);
    }

    // Indicadores superiores também passam pela validação.
    etapas.forEach(etapa => {
        etapa.setAttribute("role", "button");
        etapa.setAttribute("tabindex", "0");

        etapa.addEventListener("click", () => {
            navegarParaEtapa(Number(etapa.dataset.etapa));
        });

        etapa.addEventListener("keydown", event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                navegarParaEtapa(Number(etapa.dataset.etapa));
            }
        });
    });

    document.querySelectorAll("[data-proximo]").forEach(botao => {
        botao.addEventListener("click", () => {
            navegarParaEtapa(etapaAtual + 1);
        });
    });

    document.querySelectorAll("[data-anterior]").forEach(botao => {
        botao.addEventListener("click", () => {
            navegarParaEtapa(etapaAtual - 1);
        });
    });

    function preencherDadosPeca() {
        const opcao = pecaSelect?.selectedOptions[0];

        if (!opcao || !opcao.value || opcao.dataset.acao) {
            if (tempoInput) tempoInput.value = "";
            pecaSelect?.dispatchEvent(new Event("pecaSelecionada"));
            return;
        }

        if (tempoInput) {
            tempoInput.dataset.tempoManual = "false";
            tempoInput.value = (Number(opcao.dataset.tempo) || 0)
                * Math.min(Math.max(1, Number(pecasPorImpressaoInput?.value) || 1),
                    Math.max(1, Number(unidadesInput?.value) || 1)) || "";
        }


        pecaSelect.dispatchEvent(new Event("pecaSelecionada"));
    }

    // Comparação entre o tempo cadastrado na peça e o tempo informado.
    const resumoTempo = document.getElementById("resumoTempoImpressao");
    const statusTempo = document.getElementById("statusTempoImpressao");
    const tempoInformadoResumo = document.getElementById("tempoInformadoResumo");
    const tempoPrevistoResumo = document.getElementById("tempoPrevistoResumo");

    function atualizarResumoTempo() {
        if (!resumoTempo || !statusTempo || !tempoInput || !pecaSelect) return;

        const opcao = pecaSelect.selectedOptions[0];
        const previsto = (Number(opcao?.dataset.tempo) || 0)
            * Math.min(Math.max(1, Number(pecasPorImpressaoInput?.value) || 1),
                Math.max(1, Number(unidadesInput?.value) || 1));
        const informado = Number(tempoInput.value) || 0;

        if (!pecaSelect.value || previsto <= 0 || informado <= 0) {
            resumoTempo.hidden = true;
            return;
        }

        resumoTempo.hidden = false;
        tempoInformadoResumo.textContent = `${informado} min`;
        tempoPrevistoResumo.textContent = `${previsto} min por impressão`;

        let classe;
        let mensagem;

        if (informado < previsto) {
            classe = "consumo-abaixo";
            mensagem = "Tempo abaixo do esperado";
        } else if (informado > previsto) {
            classe = "consumo-acima";
            mensagem = "Tempo acima do esperado";
        } else {
            classe = "consumo-dentro";
            mensagem = "Tempo dentro do esperado";
        }

        statusTempo.textContent = mensagem;
        statusTempo.classList.remove(
            "consumo-dentro",
            "consumo-acima",
            "consumo-abaixo"
        );
        statusTempo.classList.add(classe);
    }

    pecaSelect?.addEventListener("change", event => {
        preencherDadosPeca();
        if (event.isTrusted) {
            const tempoTrabalhoInput = document.getElementById("tempoTrabalhoMinutos");
            if (tempoTrabalhoInput) tempoTrabalhoInput.value = "";
        }
        atualizarResumoProducao(false);
    });

    tempoInput?.addEventListener("input", event => {
        if (event.isTrusted) tempoInput.dataset.tempoManual = "true";
        atualizarResumoProducao(false);
        atualizarResumoTempo();
    });
    atualizarResumoTempo();
    // A comparação de tempo já está inicializada; agora podemos atualizar
    // o resumo sem interromper a configuração dos eventos abaixo.
    atualizarResumoProducao(true);

    const custoPrevio = document.getElementById("previaCustoUnidade");
    let temporizadorPrevia;
    async function atualizarPreviaCustos() {
        if (!custoPrevio || etapaAtual < 3) return;
        const dados = new FormData(formulario);
        // A foto não altera o custo e não deve ser reenviada em cada prévia.
        dados.delete("fotoPeca");
        dados.delete("fotoRemovida");
        try {
            const resposta = await fetch("/historico/previa", {
                method: "POST",
                body: dados,
                headers: {"X-Requested-With": "XMLHttpRequest"}
            });
            if (!resposta.ok) return;
            const valores = await resposta.json();
            custoPrevio.textContent = new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"})
                .format(Number(valores.custoUnidade));
            const lote = document.getElementById("previaCustoLote");
            if (lote) lote.textContent = new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"})
                .format(Number(valores.custoLote));
        } catch (_) {
            // A prévia é informativa; a conclusão do cálculo continua disponível.
        }
    }
    formulario.addEventListener("input", () => {
        clearTimeout(temporizadorPrevia);
        temporizadorPrevia = setTimeout(atualizarPreviaCustos, 600);
    });
    formulario.addEventListener("change", () => {
        clearTimeout(temporizadorPrevia);
        temporizadorPrevia = setTimeout(atualizarPreviaCustos, 600);
    });
    formulario.addEventListener("click", event => {
        if (event.target.closest?.("[data-confirmar-filamento]")) atualizarResumoProducao(false);
    });

    function configurarCadastroRapido(idSelect, rota) {
        const select = document.getElementById(idSelect);

        if (!select) return;

        select.addEventListener("change", () => {
            const opcao = select.selectedOptions[0];

            if (!opcao?.dataset.acao?.startsWith("cadastrar-")) {
                return;
            }

            salvarRascunho();

            const destino = `${rota}?origem=calculo`;

            if (window.Turbo) {
                window.Turbo.visit(destino);
            } else {
                window.location.href = destino;
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

    // Antes de enviar o cálculo, valida todas as etapas em ordem.
    formulario.addEventListener("submit", event => {
        for (let numero = 1; numero <= conteudos.length; numero++) {
            if (!validarEtapa(numero)) {
                event.preventDefault();
                mostrarEtapa(numero);
                return;
            }
        }

        salvarRascunho();
    });

    // Foto opcional: a miniatura substitui o botão de adicionar enquanto houver imagem.
    const fotoInput = document.getElementById("fotoPeca");
    const fotoControle = document.getElementById("fotoPecaControle");
    const fotoPreview = document.getElementById("fotoPecaPreview");
    const fotoMiniatura = document.getElementById("fotoPecaMiniatura");
    const removerFoto = document.getElementById("removerFotoPeca");
    const fotoRemovida = document.getElementById("fotoRemovida");
    let fotoUrlTemporaria = null;

    function limparFoto() {
        if (fotoInput) fotoInput.value = "";
        if (fotoRemovida) fotoRemovida.value = "true";
        if (fotoUrlTemporaria) URL.revokeObjectURL(fotoUrlTemporaria);
        fotoUrlTemporaria = null;

        fotoControle?.classList.remove("com-foto");
        fotoPreview?.classList.remove("visivel");

        if (fotoMiniatura) fotoMiniatura.removeAttribute("src");

        salvarRascunho();
    }

    function mostrarFoto(src) {
        if (!src || !fotoMiniatura) return;

        fotoMiniatura.src = src;
        fotoMiniatura.onload = () => {
            fotoPreview?.classList.add("visivel");
            fotoControle?.classList.add("com-foto");
        };
        fotoMiniatura.onerror = () => {
            fotoPreview?.classList.remove("visivel");
            fotoControle?.classList.remove("com-foto");
            window.mostrarNotificacao?.("Não foi possível exibir a prévia desta foto.", "erro");
        };
        fotoPreview?.classList.add("visivel");
        fotoControle?.classList.add("com-foto");
    }

    fotoInput?.addEventListener("change", async () => {
        const arquivo = fotoInput.files[0];

        if (!arquivo) return;

        const formatos = ["image/jpeg", "image/png", "image/webp"];

        if (!formatos.includes(arquivo.type)) {
            window.mostrarNotificacao?.("Selecione uma imagem JPG, PNG ou WEBP.", "erro");
            limparFoto();
            return;
        }

        try {
            const bitmap = await createImageBitmap(arquivo, { imageOrientation: "from-image" });
            const escala = Math.min(1, 1200 / Math.max(bitmap.width, bitmap.height));
            const canvas = document.createElement("canvas");
            canvas.width = Math.max(1, Math.round(bitmap.width * escala));
            canvas.height = Math.max(1, Math.round(bitmap.height * escala));
            const contexto = canvas.getContext("2d");
            if (!contexto) throw new Error("canvas indisponível");
            contexto.fillStyle = "#fff";
            contexto.fillRect(0, 0, canvas.width, canvas.height);
            contexto.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
            bitmap.close();
            const blob = await new Promise((resolve, reject) => canvas.toBlob(
                resultado => resultado ? resolve(resultado) : reject(new Error("compressão falhou")),
                "image/jpeg", 0.8));
            if (blob.size > 3 * 1024 * 1024) throw new Error("foto grande demais após compressão");
            const reduzido = new File([blob], "foto.jpg", { type: "image/jpeg" });
            const transferencia = new DataTransfer();
            transferencia.items.add(reduzido);
            fotoInput.files = transferencia.files;
            if (fotoRemovida) fotoRemovida.value = "false";
            if (fotoUrlTemporaria) URL.revokeObjectURL(fotoUrlTemporaria);
            fotoUrlTemporaria = URL.createObjectURL(blob);
            mostrarFoto(fotoUrlTemporaria);
            salvarRascunho();
        } catch (erro) {
            window.mostrarNotificacao?.("Não foi possível preparar essa foto. Tente selecionar outra imagem JPG, PNG ou WEBP.", "erro");
            limparFoto();
        }
    });

    removerFoto?.addEventListener("click", limparFoto);

    if (fotoMiniatura?.getAttribute("src")) fotoPreview?.classList.add("visivel");

    formulario.addEventListener("rascunhoCalculoRestaurado", evento => {
        mostrarEtapa(evento.detail.etapa || 1);
    });

    mostrarEtapa(1, false);
}

document.addEventListener("DOMContentLoaded", inicializarCalculoEtapas);
document.addEventListener("turbo:load", inicializarCalculoEtapas);
