document.addEventListener("turbo:load", () => {
    const formulario = document.getElementById("formCalculo");

    if (!formulario) return;

    const etapas = document.querySelectorAll("[data-etapa]");
    const conteudos = document.querySelectorAll("[data-conteudo]");
    const pecaSelect = document.getElementById("pecaId");
    const tempoInput = document.getElementById("tempoImpressaoMinutos");
    const plataformaSelect = document.getElementById("plataformaVenda");
    const blocosPlataforma = document.querySelectorAll("[data-opcoes-plataforma]");

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

    // Permite voltar diretamente para qualquer etapa pelo indicador superior.
    etapas.forEach(etapa => {
        etapa.setAttribute("role", "button");
        etapa.setAttribute("tabindex", "0");

        etapa.addEventListener("click", () => {
            mostrarEtapa(Number(etapa.dataset.etapa));
        });

        etapa.addEventListener("keydown", event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                mostrarEtapa(Number(etapa.dataset.etapa));
            }
        });
    });

    function validarEtapa() {
        const conteudo = document.querySelector(`[data-conteudo="${etapaAtual}"]`);

        if (!conteudo) return false;

        const campos = conteudo.querySelectorAll("input:not([type='hidden']), select, textarea");

        for (const campo of campos) {
            if (campo.disabled || campo.type === "hidden" || campo.closest("[hidden]")) continue;

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
            if (tempoInput) tempoInput.value = "";
            pecaSelect.dispatchEvent(new Event("pecaSelecionada"));
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
        statusTempo.classList.remove("consumo-dentro", "consumo-acima", "consumo-abaixo");
        statusTempo.classList.add(classe);
    }

    pecaSelect.addEventListener("change", () => {
        preencherDadosPeca();
        atualizarResumoTempo();
    });

    tempoInput?.addEventListener("input", atualizarResumoTempo);
    atualizarResumoTempo();

    function configurarCadastroRapido(idSelect, rota) {
        const select = document.getElementById(idSelect);

        if (!select) return;

        select.addEventListener("change", () => {
            const opcao = select.selectedOptions[0];

            if (opcao?.dataset.acao?.startsWith("cadastrar-")) {
                if (window.Turbo) {
                    Turbo.visit(rota);
                } else {
                    Turbo.visit(rota);
                }
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

    formulario.addEventListener("submit", event => {
        const listaFilamentos = document.getElementById("listaFilamentos");
        const itensFilamento = [...listaFilamentos.querySelectorAll("[data-filamento-item]")];

        const pendente = itensFilamento.find(item => item.dataset.confirmado !== "true");

        if (pendente) {
            event.preventDefault();
            mostrarEtapa(1);
            pendente.hidden = false;

            const select = pendente.querySelector("[data-campo-filamento]");
            const quantidade = pendente.querySelector("[data-campo-quantidade]");

            requestAnimationFrame(() => {
                if (!select.value) {
                    select.reportValidity();
                    select.focus();
                } else if (!quantidade.checkValidity()) {
                    quantidade.reportValidity();
                    quantidade.focus();
                } else {
                    alert("Confirme o filamento antes de calcular os custos.");
                    pendente.querySelector("[data-confirmar-filamento]").focus();
                }
            });

            return;
        }

        const conteudosOrdenados = [...conteudos].sort(
            (a, b) => Number(a.dataset.conteudo) - Number(b.dataset.conteudo)
        );

        for (const conteudo of conteudosOrdenados) {
            const campos = conteudo.querySelectorAll("input:not([type='hidden']), select, textarea");

            for (const campo of campos) {
                if (campo.disabled || campo.type === "hidden" || campo.closest("[hidden]")) continue;

                if (!campo.checkValidity()) {
                    event.preventDefault();
                    mostrarEtapa(Number(conteudo.dataset.conteudo));

                    requestAnimationFrame(() => {
                        campo.reportValidity();
                        campo.focus();
                    });

                    return;
                }
            }
        }
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

        leitor.onload = evento => {
            mostrarFoto(evento.target.result);
        };

        leitor.readAsDataURL(arquivo);
    });

    removerFoto?.addEventListener("click", limparFoto);

    if (fotoBase64?.value && fotoContentType?.value) {
        mostrarFoto(`data:${fotoContentType.value};base64,${fotoBase64.value}`);
    }

    // Começa pela primeira etapa, preservando os valores que o HTML renderizou.
    mostrarEtapa(1);
});