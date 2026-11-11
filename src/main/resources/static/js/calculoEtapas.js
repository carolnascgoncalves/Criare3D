
let formularioEtapasInicializado = null;

function inicializarCalculoEtapas() {
    const formulario = document.getElementById("formCalculo");

    if (!formulario || formulario === formularioEtapasInicializado) return;

    formularioEtapasInicializado = formulario;
    let temporizadorPrevia = null;
    let requisicaoPrevia = null;

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
        const unidadesInformadas = Number(unidadesInput?.value);
        const temUnidadesValidas = Number.isFinite(unidadesInformadas) && unidadesInformadas > 0;
        const unidades = temUnidadesValidas ? unidadesInformadas : 1;
        if (pecasPorImpressaoInput) {
            const limite = temUnidadesValidas ? unidades : 10000;
            pecasPorImpressaoInput.max = String(limite);
            if (temUnidadesValidas && Number(pecasPorImpressaoInput.value) > limite) {
                pecasPorImpressaoInput.value = String(limite);
            }
        }
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
        const tempoPorImpressao = Number(tempoInput?.value) || 0;
        const aquecimento = Number(impressoraSelect?.selectedOptions[0]?.dataset.tempoAquecimento) || 0;
        const minutosImpressao = tempoPorImpressao * unidades;
        const minutosTotais = minutosImpressao + aquecimento * impressoes;
        const resumoTempoTotal = document.getElementById("tempoTotalLoteResumo");
        if (resumoTempoTotal) {
            resumoTempoTotal.hidden = tempoPorImpressao <= 0;
            if (tempoPorImpressao > 0) {
                const horasTotais = Math.floor(minutosTotais / 60);
                const minutosRestantes = minutosTotais % 60;
                const tempoFormatado = horasTotais > 0
                    ? `${horasTotais} h${minutosRestantes > 0 ? ` ${minutosRestantes} min` : ""}`
                    : `${minutosRestantes} min`;
                const quantidadeFormatada = new Intl.NumberFormat("pt-BR").format(minutosTotais);
                resumoTempoTotal.textContent = `Tempo total estimado: ${quantidadeFormatada} min (${tempoFormatado}) em ${impressoes} ${impressoes === 1 ? "impressão" : "impressões"}, incluindo ${aquecimento} min de aquecimento por impressão.`;
            }
        }
        const parcial = impressoes > 1 && unidades % capacidade !== 0;
        const avisoParcial = document.getElementById("avisoImpressaoParcial");
        if (avisoParcial) {
            avisoParcial.hidden = !parcial;
            if (parcial && tempoPorImpressao > 0) {
                const minutosParciais = Math.ceil(tempoPorImpressao * (unidades % capacidade) / capacidade);
                avisoParcial.textContent = `A última impressão terá ${unidades % capacidade} peças; estimamos ${minutosParciais} min para ela (${tempoPorImpressao} min por unidade × ${unidades % capacidade} unidades).`;
            }
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
        if (numero === 4) {
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
            tempoInput.value = Number(opcao.dataset.tempo) || "";
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
        const previsto = Number(opcao?.dataset.tempo) || 0;
        const informado = Number(tempoInput.value) || 0;

        if (!pecaSelect.value || previsto <= 0 || informado <= 0) {
            resumoTempo.hidden = true;
            return;
        }

        resumoTempo.hidden = false;
        tempoInformadoResumo.textContent = `${informado} min`;
        tempoPrevistoResumo.textContent = `${previsto} min por unidade`;

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

    function atualizarAvisoAquecimento() {
        const aviso = document.getElementById("avisoAquecimentoImpressora");
        const link = document.getElementById("linkEditarAquecimento");
        const opcao = impressoraSelect?.selectedOptions[0];
        const aquecimento = Number(opcao?.dataset.tempoAquecimento) || 0;
        const selecionada = Boolean(impressoraSelect?.value);
        if (aviso) aviso.hidden = !selecionada || aquecimento > 0;
        if (link && impressoraSelect?.value) {
            link.href = `/impressora/editar/${encodeURIComponent(impressoraSelect.value)}`;
        }
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
    impressoraSelect?.addEventListener("change", () => {
        atualizarAvisoAquecimento();
        atualizarResumoProducao(false);
    });
    atualizarAvisoAquecimento();
    atualizarResumoTempo();
    // A comparação de tempo já está inicializada; agora podemos atualizar
    // o resumo sem interromper a configuração dos eventos abaixo.
    atualizarResumoProducao(true);

    const custoPrevio = document.getElementById("previaCustoUnidade");
    async function atualizarPreviaCustos() {
        if (!custoPrevio || etapaAtual !== 4) return;
        const dados = new FormData(formulario);
        // A foto não altera o custo e não deve ser reenviada em cada prévia.
        dados.delete("fotoPeca");
        dados.delete("fotoRemovida");
        requisicaoPrevia?.abort();
        const controle = new AbortController();
        requisicaoPrevia = controle;
        try {
            const resposta = await fetch("/historico/previa", {
                method: "POST",
                body: dados,
                headers: {"X-Requested-With": "XMLHttpRequest"},
                signal: controle.signal
            });
            const valores = await resposta.json().catch(() => ({}));
            if (controle.signal.aborted || requisicaoPrevia !== controle) return;
            if (!resposta.ok) {
                const mensagem = valores.erro || "Confira os dados de impressão para calcular a prévia.";
                custoPrevio.textContent = mensagem;
                const custoLote = document.getElementById("previaCustoLote");
                if (custoLote) custoLote.textContent = "—";
                custoPrevio.setAttribute("aria-invalid", "true");
                return;
            }
            custoPrevio.textContent = new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"})
                .format(Number(valores.custoUnidade));
            const lote = document.getElementById("previaCustoLote");
            if (lote) lote.textContent = new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"})
                .format(Number(valores.custoLote));
            const impressoes = Number(valores.impressoes) || 1;
            const minutos = Number(valores.minutosMaquina) || 0;
            const aquecimento = Number(valores.tempoAquecimentoPorImpressao) || 0;
            const unidadeImpressao = impressoes === 1 ? "impressão" : "impressões";
            const unidades = Number(document.getElementById("unidades")?.value) || 1;
            const info = document.getElementById("previaResumoProducao");
            const horas = Math.floor(minutos / 60);
            const minutosRestantes = minutos % 60;
            const tempoFormatado = horas > 0
                ? `${horas} h ${minutosRestantes} min`
                : `${minutosRestantes} min`;
            if (info) {
                info.textContent = `${unidades} ${unidades === 1 ? "unidade" : "unidades"} · ${impressoes} ${unidadeImpressao} · ${tempoFormatado} de máquina (inclui ${aquecimento} min de aquecimento por impressão)`;
            }
            custoPrevio.removeAttribute("aria-invalid");
        } catch (erro) {
            if (erro.name === "AbortError") return;
            custoPrevio.textContent = "Prévia temporariamente indisponível. Você ainda pode continuar o cálculo.";
            custoPrevio.setAttribute("aria-invalid", "true");
        }
    }
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
    let sincronizacaoFotoPendente = Promise.resolve();

    function sincronizarFotoTemporaria(arquivo = null, remover = false) {
        const dados = new FormData();
        const tokenCsrf = formulario.querySelector('input[name="_csrf"]')?.value;
        if (tokenCsrf) dados.append("_csrf", tokenCsrf);
        if (arquivo) dados.append("fotoPeca", arquivo);
        if (remover) dados.append("remover", "true");

        sincronizacaoFotoPendente = sincronizacaoFotoPendente
            .catch(() => {})
            .then(async () => {
                const resposta = await fetch("/historico/rascunho/foto", {
                    method: "POST",
                    body: dados,
                    credentials: "same-origin"
                });
                if (!resposta.ok) throw new Error("Não foi possível guardar a foto temporária.");
            });

        sincronizacaoFotoPendente.catch(() => {
            window.mostrarNotificacao?.(
                "Não foi possível guardar a foto para continuar depois. Tente selecioná-la novamente.",
                "erro"
            );
        });
    }

    function limparFoto() {
        if (fotoInput) fotoInput.value = "";
        if (fotoRemovida) fotoRemovida.value = "true";
        sincronizarFotoTemporaria(null, true);
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
            sincronizarFotoTemporaria(reduzido);
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
