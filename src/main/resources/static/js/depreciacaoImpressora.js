(() => {
    const raiz = document.querySelector("[data-estimativa-depreciacao]");
    if (!raiz) return;

    const modelo = document.getElementById("modelo");
    const horas = document.getElementById("vidaUtilHoras");
    const horasMensais = document.getElementById("horasMensaisUso");
    const horasMensaisManual = document.getElementById("horasMensaisManual");
    const custoMensal = document.getElementById("custoDepreciacaoMensal");
    const custoHora = document.getElementById("depreciacaoPorHora");
    const origem = document.getElementById("depreciacaoOrigem");
    const anunciosNovos = document.getElementById("anunciosNovos");
    const anunciosUsados = document.getElementById("anunciosUsados");
    const manual = document.getElementById("depreciacaoManual");
    const botao = document.getElementById("buscarReferenciaDepreciacao");
    const aviso = document.getElementById("referenciaDepreciacaoMensagem");
    const resumo = document.getElementById("referenciaDepreciacaoResumo");
    let ultimaEstimativa = null;
    let horasMensaisEditadas = horasMensaisManual.value === "true";
    let horasVidaEditadas = false;
    let requisicaoAtual = null;

    function atualizarCustoMensal() {
        const taxa = Number(custoHora.value);
        const uso = Number(horasMensais.value);
        if (Number.isFinite(taxa) && taxa >= 0 && Number.isFinite(uso) && uso >= 0) {
            const mensal = taxa * uso;
            custoMensal.textContent = `Depreciação estimada no mês: ${mensal.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}. Uso mensal é uma estimativa e pode ser ajustado.`;
        }
    }

    function atualizarCustoHora() {
        if (!ultimaEstimativa || manual.value === "true") return;
        const divisor = Number(horas.value || ultimaEstimativa.horasAteSubstituicao);
        if (!Number.isFinite(divisor) || divisor <= 0 || ultimaEstimativa.precoNovoMediano == null) return;
        const base = ultimaEstimativa.fallback || ultimaEstimativa.precoUsadoMediano == null
            ? Number(ultimaEstimativa.precoNovoMediano)
            : Math.max(0, Number(ultimaEstimativa.precoNovoMediano) - Number(ultimaEstimativa.precoUsadoMediano));
        custoHora.value = (base / divisor).toFixed(8);
        origem.value = ultimaEstimativa.fallback ? "MERCADO_LIVRE_RESERVA" : "MERCADO_LIVRE";
        anunciosNovos.value = ultimaEstimativa.anunciosNovos;
        anunciosUsados.value = ultimaEstimativa.anunciosUsados;
        atualizarCustoMensal();
    }

    async function buscarReferencia() {
        const valorModelo = modelo.value.trim();
        if (valorModelo.length < 2) {
            aviso.hidden = true;
            window.mostrarNotificacao?.("Selecione ou informe o modelo da impressora.", "erro");
            window.validacaoInline?.destacar(modelo);
            return;
        }
        botao.disabled = true;
        botao.textContent = "Buscando referência…";
        aviso.hidden = true;
        resumo.hidden = true;
        try {
            const parametros = new URLSearchParams({ modelo: valorModelo });
            if (raiz.dataset.impressoraId) parametros.set("impressoraId", raiz.dataset.impressoraId);
            requisicaoAtual?.abort();
            requisicaoAtual = new AbortController();
            const resposta = await fetch(`/api/impressoras/depreciacao/estimativa?${parametros}`, {
                headers: { Accept: "application/json" },
                credentials: "same-origin",
                signal: requisicaoAtual.signal
            });
            const dados = await resposta.json();
            if (!resposta.ok) throw new Error(dados.message || dados.mensagem || "Não foi possível buscar a referência.");
            if (!dados.disponivel) {
                if (!horasVidaEditadas && !horas.value) horas.value = dados.horasAteSubstituicao;
                if (!horasMensaisEditadas) horasMensais.value = dados.horasMensais;
                atualizarCustoMensal();
                throw new Error(dados.mensagem);
            }
            ultimaEstimativa = dados;
            if (!horasVidaEditadas) horas.value = dados.horasAteSubstituicao;
            if (!horasMensaisEditadas) horasMensais.value = dados.horasMensais;
            atualizarCustoHora();
            const novo = dados.precoNovoMediano == null ? "sem amostra suficiente" : `novo: ${Number(dados.precoNovoMediano).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })} (${dados.anunciosNovos} anúncios)`;
            const usado = dados.precoUsadoMediano == null ? "usado: amostra insuficiente" : `usado: ${Number(dados.precoUsadoMediano).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })} (${dados.anunciosUsados} anúncios)`;
            const usoInfo = dados.horasMensaisPadrao
                ? `Uso mensal inicial: ${Number(dados.horasMensais).toLocaleString("pt-BR")} h (padrão editável, sem histórico nos últimos 30 dias)`
                : `Uso observado nos últimos 30 dias: ${Number(dados.horasMensais).toLocaleString("pt-BR")} h/mês`;
            resumo.textContent = `${novo} · ${usado} · ${dados.confianca}. ${usoInfo}.`;
            if (dados.horasMensaisPadrao && !horasMensaisEditadas) {
                horasMensais.value = dados.horasMensais;
            }
            resumo.hidden = false;
            aviso.textContent = dados.mensagem;
            aviso.hidden = false;
        } catch (erro) {
            if (erro.name === "AbortError") return;
            aviso.hidden = true;
            window.mostrarNotificacao?.(erro.message || "Não foi possível consultar o Mercado Livre agora.", "erro");
        } finally {
            botao.disabled = false;
            botao.innerHTML = '<i class="bi bi-search"></i> Buscar referência de preços';
        }
    }

    botao.addEventListener("click", buscarReferencia);
    horas.addEventListener("input", () => {
        horasVidaEditadas = true;
        atualizarCustoHora();
        atualizarCustoMensal();
    });
    horasMensais.addEventListener("input", () => {
        horasMensaisEditadas = true;
        horasMensaisManual.value = "true";
        atualizarCustoMensal();
    });
    custoHora.addEventListener("input", () => {
        manual.value = "true";
        origem.value = "MANUAL";
        aviso.textContent = "Valor editado manualmente. A referência automática não substituirá este valor.";
        aviso.hidden = false;
        atualizarCustoMensal();
    });
    modelo.addEventListener("change", () => {
        ultimaEstimativa = null;
        if (!horasVidaEditadas) horas.value = "";
        if (manual.value !== "true") {
            custoHora.value = "";
            origem.value = "";
            anunciosNovos.value = "";
            anunciosUsados.value = "";
            resumo.hidden = true;
        }
        if (modelo.value.trim().length >= 2) buscarReferencia();
    });
    atualizarCustoMensal();
})();
