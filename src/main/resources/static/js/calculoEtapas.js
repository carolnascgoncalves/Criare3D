
let formularioEtapasInicializado = null;

function inicializarCalculoEtapas() {
    const formulario = document.getElementById("formCalculo");

    if (!formulario || formulario === formularioEtapasInicializado) return;

    formularioEtapasInicializado = formulario;

    const etapas = document.querySelectorAll("[data-etapa]");
    const conteudos = document.querySelectorAll("[data-conteudo]");
    const pecaSelect = document.getElementById("pecaId");
    const tempoInput = document.getElementById("tempoImpressaoMinutos");
    const impressoraSelect = document.getElementById("impressoraId");
    const embalagemSelect = document.getElementById("embalagemId");
    const listaFilamentos = document.getElementById("listaFilamentos");
    const plataformaSelect = document.getElementById("plataformaVenda");
    const blocosPlataforma = document.querySelectorAll("[data-opcoes-plataforma]");

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
            alert("Adicione pelo menos um filamento.");
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

            alert("Confirme todos os filamentos antes de avançar.");
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
                alert("Confirme pelo menos um filamento antes de avançar.");
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
            tempoInput.value = opcao.dataset.tempo || "";
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
        tempoPrevistoResumo.textContent = `${previsto} min`;

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
        atualizarResumoTempo();
    });

    tempoInput?.addEventListener("input", atualizarResumoTempo);
    atualizarResumoTempo();

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
    const fotoBase64 = document.getElementById("fotoPecaBase64");
    const fotoContentType = document.getElementById("fotoPecaContentType");

    function limparFoto() {
        if (fotoInput) fotoInput.value = "";
        if (fotoBase64) fotoBase64.value = "";
        if (fotoContentType) fotoContentType.value = "";

        fotoControle?.classList.remove("com-foto");
        fotoPreview?.classList.remove("visivel");

        if (fotoMiniatura) fotoMiniatura.removeAttribute("src");

        salvarRascunho();
    }

    function mostrarFoto(src) {
        if (!src || !fotoMiniatura) return;

        fotoMiniatura.src = src;
        fotoPreview?.classList.add("visivel");
        fotoControle?.classList.add("com-foto");
    }

    fotoInput?.addEventListener("change", () => {
        const arquivo = fotoInput.files[0];

        if (!arquivo) return;

        if (fotoBase64) fotoBase64.value = "";
        if (fotoContentType) fotoContentType.value = "";

        const formatos = ["image/jpeg", "image/png", "image/webp"];

        if (!formatos.includes(arquivo.type)) {
            alert("Selecione uma imagem JPG, PNG ou WEBP.");
            limparFoto();
            return;
        }

        if (arquivo.size > 3 * 1024 * 1024) {
            alert("A foto deve ter no máximo 3 MB.");
            limparFoto();
            return;
        }

        const leitor = new FileReader();

        leitor.onerror = () => {
            alert("Não foi possível abrir essa foto. Tente escolher outra.");
            limparFoto();
        };

        leitor.onload = evento => {
            const imagem = new Image();

            imagem.onerror = () => {
                alert("Não foi possível abrir essa foto. Tente escolher outra.");
                limparFoto();
            };

            imagem.onload = () => {
                const limite = 1000;
                const escala = Math.min(1, limite / Math.max(imagem.width, imagem.height));
                const canvas = document.createElement("canvas");
                canvas.width = Math.max(1, Math.round(imagem.width * escala));
                canvas.height = Math.max(1, Math.round(imagem.height * escala));

                const contexto = canvas.getContext("2d");
                if (!contexto) {
                    alert("Não foi possível preparar essa foto. Tente outra imagem.");
                    limparFoto();
                    return;
                }

                // JPEG não preserva transparência; usar fundo branco evita áreas pretas.
                contexto.fillStyle = "#fff";
                contexto.fillRect(0, 0, canvas.width, canvas.height);
                contexto.drawImage(imagem, 0, 0, canvas.width, canvas.height);

                const resultado = canvas.toDataURL("image/jpeg", 0.82);
                mostrarFoto(resultado);

                if (fotoBase64) fotoBase64.value = resultado.split(",")[1] || "";
                if (fotoContentType) fotoContentType.value = "image/jpeg";

                salvarRascunho();
            };

            imagem.src = evento.target.result;
        };

        leitor.readAsDataURL(arquivo);
    });

    removerFoto?.addEventListener("click", limparFoto);

    if (fotoBase64?.value && fotoContentType?.value) {
        mostrarFoto(`data:${fotoContentType.value};base64,${fotoBase64.value}`);
    }

    formulario.addEventListener("rascunhoCalculoRestaurado", evento => {
        mostrarEtapa(evento.detail.etapa || 1);
    });

    mostrarEtapa(1, false);
}

document.addEventListener("DOMContentLoaded", inicializarCalculoEtapas);
document.addEventListener("turbo:load", inicializarCalculoEtapas);
