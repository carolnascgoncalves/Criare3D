(function () {
    function inicializarTarifaEnergiaReferencia() {
        const bloco = document.getElementById("tarifaReferencia");
        if (!bloco || bloco.dataset.inicializado === "true") return;
        bloco.dataset.inicializado = "true";

        const botaoCarregar = document.getElementById("carregarTarifasAneel");
        const botaoLocalizacao = document.getElementById("sugerirLocalizacaoTarifa");
        const botaoAlternar = document.getElementById("alternarTarifaReferencia");
        const conteudoReferencia = document.getElementById("tarifaReferenciaConteudo");
        const select = document.getElementById("distribuidoraTarifa");
        const campoTarifa = document.getElementById("tarifaEnergiaKwh");
        const mensagem = document.getElementById("tarifaReferenciaMensagem");
        const textoLocalizacao = document.getElementById("localizacaoTarifaTexto");
        const fonte = document.getElementById("tarifaReferenciaFonte");

        function informar(texto, erro = false) {
            if (!mensagem) return;
            mensagem.classList.toggle("tarifa-referencia-erro", erro);
            mensagem.innerHTML = `<i class="bi ${erro ? "bi-exclamation-circle" : "bi-info-circle"}"></i> `;
            mensagem.append(document.createTextNode(texto));
        }

        botaoAlternar?.addEventListener("click", () => {
            const aberto = botaoAlternar.getAttribute("aria-expanded") !== "true";
            botaoAlternar.setAttribute("aria-expanded", String(aberto));
            if (conteudoReferencia) conteudoReferencia.hidden = !aberto;
        });

        botaoCarregar?.addEventListener("click", async () => {
            botaoCarregar.disabled = true;
            botaoCarregar.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Consultando…';
            informar("Buscando as tarifas residenciais vigentes na ANEEL. Isso pode levar alguns segundos.");
            try {
                const resposta = await fetch(bloco.dataset.endpoint, {
                    headers: { Accept: "application/json" },
                    credentials: "same-origin"
                });
                const dados = await resposta.json();
                if (!resposta.ok) throw new Error(dados.erro || "Não foi possível consultar as tarifas agora.");
                const tarifas = Array.isArray(dados.tarifas) ? dados.tarifas : [];
                select.replaceChildren(new Option(tarifas.length ? "Selecione a distribuidora da sua conta" : "Nenhuma tarifa vigente encontrada", ""));
                tarifas.forEach(tarifa => {
                    const vigencia = tarifa.inicioVigencia
                        ? ` · desde ${new Date(`${tarifa.inicioVigencia}T12:00:00`).toLocaleDateString("pt-BR")}` : "";
                    const option = new Option(`${tarifa.distribuidora}${vigencia}`, tarifa.distribuidora);
                    option.dataset.valor = tarifa.valorKwh;
                    option.dataset.inicio = tarifa.inicioVigencia || "";
                    option.dataset.fim = tarifa.fimVigencia || "";
                    select.add(option);
                });
                select.disabled = tarifas.length === 0;
                if (fonte) fonte.hidden = false;
                informar(tarifas.length
                    ? `${tarifas.length} referências residenciais vigentes carregadas. Confira o nome da distribuidora na sua conta de luz.`
                    : "A ANEEL não retornou referências vigentes. Você ainda pode informar a tarifa manualmente.", !tarifas.length);
            } catch (erro) {
                informar(erro.message || "Falha ao consultar as tarifas. Tente novamente ou preencha o valor manualmente.", true);
            } finally {
                botaoCarregar.disabled = false;
            botaoCarregar.innerHTML = '<i class="bi bi-arrow-down-circle"></i> Carregar tarifas de SP';
            }
        });

        select?.addEventListener("change", () => {
            const option = select.selectedOptions[0];
            const valor = Number(option?.dataset.valor);
            if (!option?.value || !Number.isFinite(valor) || valor <= 0 || !campoTarifa) return;
            campoTarifa.value = valor.toFixed(2);
            campoTarifa.dispatchEvent(new Event("input", { bubbles: true }));
            campoTarifa.dispatchEvent(new Event("change", { bubbles: true }));
            const inicio = option.dataset.inicio ? new Date(`${option.dataset.inicio}T12:00:00`).toLocaleDateString("pt-BR") : "";
            const fim = option.dataset.fim ? new Date(`${option.dataset.fim}T12:00:00`).toLocaleDateString("pt-BR") : "vigente";
            informar(`Referência de ${option.value}: R$ ${valor.toFixed(2).replace(".", ",")}/kWh (TE + TUSD), válida de ${inicio} a ${fim}. O valor foi preenchido, mas você pode ajustá-lo antes de salvar.`);
        });

        botaoLocalizacao?.addEventListener("click", () => {
            if (!navigator.geolocation) {
                textoLocalizacao.textContent = "Este navegador não oferece localização. Confira a cidade/UF na conta de luz.";
                return;
            }
            botaoLocalizacao.disabled = true;
            textoLocalizacao.textContent = "Solicitando localização…";
            navigator.geolocation.getCurrentPosition(async posicao => {
                try {
                    const tokenCsrf = document.querySelector('meta[name="csrf-token"]')?.content;
                    const resposta = await fetch(`${bloco.dataset.endpoint}/localizacao`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": tokenCsrf || "" },
                        credentials: "same-origin",
                        body: JSON.stringify({ latitude: posicao.coords.latitude, longitude: posicao.coords.longitude })
                    });
                    const dados = await resposta.json();
                    if (!resposta.ok) throw new Error(dados.erro || "Falha ao identificar cidade.");
                    textoLocalizacao.textContent = `${dados.cidade} / ${dados.uf}`;
                } catch {
                    textoLocalizacao.textContent = "Não foi possível identificar a cidade. Confira a distribuidora diretamente na conta de luz.";
                } finally {
                    botaoLocalizacao.disabled = false;
                }
            }, erro => {
                textoLocalizacao.textContent = erro.code === 1
                    ? "Localização não permitida. Confira a distribuidora na conta de luz; a escolha manual continua disponível."
                    : "Não foi possível obter a localização. Confira a distribuidora na conta de luz.";
                botaoLocalizacao.disabled = false;
            }, { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 });
        });
    }

    document.addEventListener("DOMContentLoaded", inicializarTarifaEnergiaReferencia);
    document.addEventListener("turbo:load", inicializarTarifaEnergiaReferencia);
    document.addEventListener("turbo:before-cache", () => {
        const bloco = document.getElementById("tarifaReferencia");
        if (bloco) delete bloco.dataset.inicializado;
    });
})();
