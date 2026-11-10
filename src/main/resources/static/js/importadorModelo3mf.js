function inicializarImportadorModelo3mf() {
    const secao = document.querySelector("[data-importador-3mf]");
    if (!secao || secao.dataset.inicializado === "true") return;
    const formularioPeca = secao.parentElement.querySelector("form");
    if (secao.dataset.inicializado !== "true") {
        secao.dataset.inicializado = "true";
        inicializarImportador3mf(secao, formularioPeca);
    }
}

function inicializarImportador3mf(secao, formularioPeca) {
    const layout = secao.closest(".peca-cadastro-layout") || secao.parentElement;
    const input = secao.querySelector("#arquivoModelo3mf");
    const botao = secao.querySelector("[data-analisar-3mf]");
    const mensagem = secao.querySelector("[data-mensagem-3mf]");
    const dados = layout.querySelector("[data-dados-3mf]");
    const lista = layout.querySelector("[data-lista-dados-3mf]");
    const avisoFatiamento = layout.querySelector("[data-aviso-fatiamento-3mf]");
    const placas = secao.querySelector("[data-placas-3mf]");
    const resumoFatiamento = layout.querySelector("[data-resumo-estatisticas]");
    const totaisFatiamento = layout.querySelector("[data-totais-fatiamento]");
    const filamentosFatiamento = layout.querySelector("[data-filamentos-fatiamento]");
    const usarTempoFatiado = layout.querySelector("[data-usar-tempo-fatiado]");
    const nomePeca = formularioPeca?.querySelector("#nome");
    const filamentoPeca = formularioPeca?.querySelector("#quantidadeFilamentoGramas");
    const tempoPeca = formularioPeca?.querySelector("#tempoImpressaoMinutos");
    const origemNome = formularioPeca?.querySelector("[data-origem-nome]");
    const origemFilamento = formularioPeca?.querySelector("[data-origem-filamento]");
    const origemTempo = formularioPeca?.querySelector("[data-origem-tempo]");

    function informar(texto, erro = false) {
        mensagem.textContent = texto;
        mensagem.hidden = !texto;
        mensagem.classList.toggle("erro", erro);
    }

    function notificarLeitura(texto) {
        informar("");
        window.mostrarNotificacao?.(texto, "sucesso");
    }

    usarTempoFatiado?.addEventListener("click", () => {
        const segundos = Number(resumoFatiamento?.dataset.tempoTotalSegundos);
        const gramas = Number(resumoFatiamento?.dataset.pesoTotalGramas);
        if (!Number.isFinite(segundos) || segundos <= 0) return;
        aplicarValor(tempoPeca, Math.ceil(segundos / 60), origemTempo);
        if (Number.isFinite(gramas) && gramas > 0) aplicarValor(filamentoPeca, formatarNumeroCampo(gramas), origemFilamento);
        informar(`Tempo${Number.isFinite(gramas) && gramas > 0 ? " e filamento" : ""} aplicados ao cadastro. Confira se os totais representam uma unidade do produto.`);
    });

    botao.addEventListener("click", async () => {
        const arquivo = input.files?.[0];
        if (!arquivo) {
            informar("Escolha um arquivo .3mf primeiro.", true);
            return;
        }
        const nome = arquivo.name.toLowerCase();
        if (!nome.endsWith(".3mf")) {
            informar("Escolha um arquivo de projeto .3mf ou de impressão fatiada .gcode.3mf.", true);
            return;
        }
        if (arquivo.size > 50 * 1024 * 1024) {
            informar("O arquivo deve ter no máximo 50 MB.", true);
            return;
        }

        botao.disabled = true;
        dados.hidden = true;
        placas.hidden = true;
        [origemNome, origemFilamento, origemTempo].forEach(indicador => {
            if (indicador) indicador.hidden = true;
        });
        informar("Lendo o projeto 3D…");
        try {
            const formData = new FormData();
            const csrf = formularioPeca?.querySelector('input[name="_csrf"]')?.value;
            if (csrf) formData.append("_csrf", csrf);
            formData.append("arquivo3mf", arquivo);
            const resposta = await fetch("/peca/arquivo-3mf/analisar", {
                method: "POST",
                body: formData,
                credentials: "same-origin",
                headers: {"X-Requested-With": "XMLHttpRequest"}
            });
            const corpo = await resposta.json().catch(() => ({}));
            if (!resposta.ok) throw new Error(corpo.erro || "Não foi possível ler esse projeto 3MF.");

            if (corpo.nome && nomePeca) nomePeca.value = corpo.nome;
            if (corpo.nome && origemNome) origemNome.hidden = false;
            lista.replaceChildren();
            const campos = [
                ["Impressora do perfil", corpo.modeloImpressora],
                ["Material configurado", formatarMateriais(corpo.material)],
                ["Cor configurada", corpo.corMaterial],
                ["Altura de camada", corpo.alturaCamada ? `${corpo.alturaCamada} mm` : ""],
                ["Diâmetro do bico", corpo.diametroBico ? `${corpo.diametroBico} mm` : ""],
                ["Perfil de impressão", corpo.perfil],
                ["Placas / impressões", corpo.numeroPlacas > 0 ? `${corpo.numeroPlacas} ${corpo.numeroPlacas === 1 ? "placa" : "placas"} · ${corpo.numeroPlacas} ${corpo.numeroPlacas === 1 ? "impressão" : "impressões"}` : ""],
                ["Aplicativo", corpo.fatiador]
            ];
            campos.forEach(([rotulo, valor]) => {
                if (!valor) return;
                const termo = document.createElement("dt");
                termo.textContent = rotulo;
                const descricao = document.createElement("dd");
                if (rotulo === "Perfil de impressão" && /\bwalls?\b/i.test(valor)) {
                    descricao.textContent = valor;
                    descricao.title = "Walls indica paredes ou perímetros do modelo; não é o número de placas.";
                } else if (rotulo === "Material configurado" && valor) {
                    descricao.textContent = valor;
                } else if (rotulo === "Cor configurada") {
                    const cores = valor.split(",").map(cor => cor.trim()).filter(cor => /^#(?:[\da-f]{3}|[\da-f]{6})$/i.test(cor));
                    if (cores.length) {
                        const amostras = document.createElement("span");
                        amostras.className = "importacao-peca-amostras-cor";
                        cores.forEach((cor, indice) => {
                            const amostra = document.createElement("span");
                            amostra.className = "importacao-peca-amostra-cor";
                            amostra.style.backgroundColor = cor;
                            amostra.title = `Cor ${indice + 1}: ${cor}`;
                            amostra.setAttribute("aria-label", `Cor ${indice + 1}, código ${cor}`);
                            amostras.appendChild(amostra);
                        });
                        descricao.append(amostras);
                    } else {
                        descricao.textContent = valor;
                    }
                } else {
                    descricao.textContent = valor;
                }
                lista.append(termo, descricao);
            });
            renderizarPlacas(placas, corpo.placas || [], Number(corpo.numeroPlacas || 0));
            renderizarResumoFatiamento(corpo, resumoFatiamento, totaisFatiamento, filamentosFatiamento);
            if (corpo.temEstatisticas && corpo.estatisticasCompletas) {
                const segundosTotais = Number(corpo.tempoTotalSegundos);
                if (tempoPeca && Number.isFinite(segundosTotais) && segundosTotais > 0) {
                    aplicarValor(tempoPeca, Math.ceil(segundosTotais / 60), origemTempo);
                }
                const gramasTotais = Number(corpo.pesoTotalGramas);
                if (filamentoPeca && Number.isFinite(gramasTotais) && gramasTotais > 0) {
                    aplicarValor(filamentoPeca, formatarNumeroCampo(gramasTotais), origemFilamento);
                }
                avisoFatiamento.textContent = "Estatísticas encontradas para todas as placas. Tempo e consumo total foram preenchidos; confira se representam uma unidade do produto.";
            } else if (corpo.temEstatisticas) {
                avisoFatiamento.textContent = `Estatísticas encontradas para ${corpo.numeroPlacasComEstatisticas} de ${corpo.numeroPlacas} placas. O tempo não foi preenchido porque o total está incompleto.`;
            } else if (corpo.jaFatiado) {
                avisoFatiamento.textContent = "O arquivo parece fatiado, mas não trouxe tempo e consumo. No Bambu Studio, exporte o arquivo fatiado (.gcode.3mf). No OrcaSlicer, use uma versão que inclua a opção ‘Export statistics with G-code’ e exporte novamente.";
            } else {
                avisoFatiamento.textContent = "Este projeto ainda não foi fatiado. Fatie no Bambu Studio ou OrcaSlicer e exporte o arquivo fatiado (.gcode.3mf) para incluir tempo e consumo.";
            }
            dados.hidden = false;
            if (corpo.temEstatisticas && corpo.estatisticasCompletas) {
                const temPeso = Number(corpo.pesoTotalGramas) > 0;
                notificarLeitura(`Projeto lido. Nome e tempo (${formatarDuracao(corpo.tempoTotalSegundos)})${temPeso ? " e consumo total" : ""} foram aplicados ao cadastro; confira se correspondem a uma unidade.`);
            } else if (corpo.temEstatisticas) {
                notificarLeitura(`Projeto lido. Encontradas estatísticas de ${corpo.numeroPlacasComEstatisticas} de ${corpo.numeroPlacas} placas; o tempo não foi preenchido porque os dados estão incompletos.`);
            } else {
                notificarLeitura(`Projeto lido. O nome “${corpo.nome}” foi colocado no cadastro; você pode editá-lo.`);
            }
        } catch (erro) {
            informar(erro.message || "Não foi possível ler esse projeto 3MF.", true);
        } finally {
            botao.disabled = false;
        }
    });
}

function aplicarValor(campo, valor, indicadorOrigem) {
    if (!campo || valor == null || valor === "") return;
    campo.value = String(valor);
    if (indicadorOrigem) indicadorOrigem.hidden = false;
    campo.dispatchEvent(new Event("input", {bubbles: true}));
    campo.dispatchEvent(new Event("change", {bubbles: true}));
}

function formatarNumeroCampo(valor) {
    return Number(valor).toLocaleString("en-US", {useGrouping: false, maximumFractionDigits: 2});
}

function formatarMateriais(valor) {
    const contagem = new Map();
    String(valor || "").split(",").map(item => item.trim()).filter(Boolean).forEach(item => {
        const chave = item.toLocaleLowerCase("pt-BR");
        const atual = contagem.get(chave);
        contagem.set(chave, {nome: atual?.nome || item, quantidade: (atual?.quantidade || 0) + 1});
    });
    return [...contagem.values()].map(item => `${item.nome} (${item.quantidade} ${item.quantidade === 1 ? "material" : "materiais"})`).join(", ");
}

function renderizarPlacas(container, previews, total) {
    container.replaceChildren();
    const porNumero = new Map((Array.isArray(previews) ? previews : []).map(placa => [Number(placa.numero), placa]));
    const quantidade = Math.min(Math.max(total || porNumero.size, porNumero.size, 1), 8);
    const lista = Array.from({length: quantidade}, (_, indice) => {
        const numero = indice + 1;
        return {numero, ...(porNumero.get(numero) || {})};
    });
    const galeria = document.createElement("div");
    galeria.className = "importacao-peca-galeria";
    const principal = document.createElement("figure");
    principal.className = "importacao-peca-placa-principal";
    const miniaturas = document.createElement("div");
    miniaturas.className = "importacao-peca-miniaturas";
    miniaturas.setAttribute("role", "group");
    miniaturas.setAttribute("aria-label", "Escolher prévia da placa");

    function mostrarPlaca(placa, imagemElemento) {
        principal.replaceChildren();
        const uri = placa.imagemPreviewDataUri;
        if (typeof uri === "string" && /^data:image\/(?:png|jpeg|webp);base64,/i.test(uri)) {
            imagemElemento.src = uri;
            imagemElemento.alt = `Prévia da placa ${placa.numero}`;
            principal.appendChild(imagemElemento);
        } else {
            const vazio = document.createElement("div");
            vazio.className = "importacao-peca-placa-vazia";
            vazio.innerHTML = '<i class="bi bi-image" aria-hidden="true"></i><span>Prévia indisponível</span>';
            principal.appendChild(vazio);
        }
        const legenda = document.createElement("figcaption");
        legenda.textContent = `Placa ${placa.numero} de ${quantidade}`;
        principal.appendChild(legenda);
        const detalhes = [
            placa.tempoSegundos != null ? formatarDuracao(Number(placa.tempoSegundos)) : "",
            placa.pesoGramas != null ? `${formatarDecimal(placa.pesoGramas)} g` : ""
        ].filter(Boolean).join(" · ");
        if (detalhes) {
            const resumo = document.createElement("small");
            resumo.textContent = detalhes;
            principal.appendChild(resumo);
        }
    }

    lista.forEach((placa, indice) => {
        const botao = document.createElement("button");
        botao.type = "button";
        botao.className = "importacao-peca-miniatura";
        botao.setAttribute("aria-label", `Mostrar prévia da placa ${placa.numero}`);
        botao.setAttribute("aria-pressed", indice === 0 ? "true" : "false");
        const imagem = document.createElement("img");
        const uri = placa.imagemPreviewDataUri;
        if (typeof uri === "string" && /^data:image\/(?:png|jpeg|webp);base64,/i.test(uri)) {
            imagem.src = uri;
            imagem.alt = "";
        } else {
            imagem.hidden = true;
            const semImagem = document.createElement("span");
            semImagem.className = "importacao-peca-miniatura-vazia";
            semImagem.innerHTML = '<i class="bi bi-image" aria-hidden="true"></i>';
            botao.appendChild(semImagem);
        }
        if (!imagem.hidden) botao.appendChild(imagem);
        const rotulo = document.createElement("span");
        rotulo.textContent = String(placa.numero);
        botao.appendChild(rotulo);
        botao.addEventListener("click", () => {
            miniaturas.querySelectorAll("button").forEach(item => item.setAttribute("aria-pressed", "false"));
            botao.setAttribute("aria-pressed", "true");
            mostrarPlaca(placa, document.createElement("img"));
        });
        miniaturas.appendChild(botao);
    });

    mostrarPlaca(lista[0], document.createElement("img"));
    galeria.append(principal);
    if (quantidade > 1) galeria.appendChild(miniaturas);
    container.appendChild(galeria);
    container.hidden = false;
}

function renderizarResumoFatiamento(corpo, painel, totais, listaFilamentos) {
    const temDados = Boolean(corpo.temEstatisticas && Number(corpo.tempoTotalSegundos) > 0);
    painel.hidden = !temDados;
    painel.dataset.tempoTotalSegundos = temDados ? String(corpo.tempoTotalSegundos) : "";
    const botaoTempo = painel.querySelector("[data-usar-tempo-fatiado]");
    if (botaoTempo) botaoTempo.disabled = !corpo.estatisticasCompletas;
    painel.dataset.pesoTotalGramas = temDados && Number(corpo.pesoTotalGramas) > 0 ? String(corpo.pesoTotalGramas) : "";
    totais.replaceChildren();
    listaFilamentos.replaceChildren();
    if (!temDados) return;

    totais.textContent = `${formatarDuracao(corpo.tempoTotalSegundos)} em ${corpo.numeroPlacasComEstatisticas} de ${corpo.numeroPlacas} placas · ${formatarDecimal(corpo.pesoTotalGramas)} g de filamento`;
    (Array.isArray(corpo.filamentosUsados) ? corpo.filamentosUsados : []).forEach(filamento => {
        const item = document.createElement("li");
        const amostra = document.createElement("span");
        amostra.className = "importacao-peca-amostra-cor";
        if (/^#(?:[\da-f]{3}|[\da-f]{6})$/i.test(filamento.cor || "")) {
            amostra.style.backgroundColor = filamento.cor;
            amostra.title = filamento.cor;
            amostra.setAttribute("aria-label", `Cor ${filamento.cor}`);
        } else {
            amostra.hidden = true;
        }
        const texto = document.createElement("span");
        texto.textContent = `${filamento.material || `Filamento ${filamento.id}`}: ${formatarDecimal(filamento.pesoGramas)} g`;
        item.append(amostra, texto);
        listaFilamentos.appendChild(item);
    });
}

function formatarDuracao(segundos) {
    const minutosTotal = Math.ceil(Number(segundos || 0) / 60);
    const horas = Math.floor(minutosTotal / 60);
    const minutos = minutosTotal % 60;
    if (horas && minutos) return `${horas} h ${minutos} min`;
    if (horas) return `${horas} h`;
    return `${minutos} min`;
}

function formatarDecimal(valor) {
    return Number(valor || 0).toLocaleString("pt-BR", {maximumFractionDigits: 2});
}

document.addEventListener("DOMContentLoaded", inicializarImportadorModelo3mf);
document.addEventListener("turbo:load", inicializarImportadorModelo3mf);
