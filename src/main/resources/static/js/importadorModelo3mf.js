let importadorModelo3mfInicializado = null;

function inicializarImportadorModelo3mf() {
    const secao = document.querySelector("[data-importador-3mf]");
    if (!secao || secao === importadorModelo3mfInicializado) return;
    importadorModelo3mfInicializado = secao;

    const input = secao.querySelector("#arquivoModelo3mf");
    const botao = secao.querySelector("[data-analisar-3mf]");
    const mensagem = secao.querySelector("[data-mensagem-3mf]");
    const dados = secao.querySelector("[data-dados-3mf]");
    const lista = secao.querySelector("[data-lista-dados-3mf]");
    const avisoFatiamento = secao.querySelector("[data-aviso-fatiamento-3mf]");
    const preview = secao.querySelector("[data-preview-3mf]");
    const layoutDados = secao.querySelector("[data-layout-3mf]");
    const formularioPeca = secao.parentElement.querySelector("form");
    const nomePeca = formularioPeca?.querySelector("#nome");

    function informar(texto, erro = false) {
        mensagem.textContent = texto;
        mensagem.hidden = !texto;
        mensagem.classList.toggle("erro", erro);
    }

    botao.addEventListener("click", async () => {
        const arquivo = input.files?.[0];
        if (!arquivo) {
            informar("Escolha um arquivo .3mf primeiro.", true);
            return;
        }
        const nome = arquivo.name.toLowerCase();
        if (!nome.endsWith(".3mf") || nome.endsWith(".gcode.3mf")) {
            informar("Escolha o arquivo do projeto .3mf. O arquivo .gcode.3mf será tratado depois.", true);
            return;
        }
        if (arquivo.size > 20 * 1024 * 1024) {
            informar("O arquivo deve ter no máximo 20 MB.", true);
            return;
        }

        botao.disabled = true;
        dados.hidden = true;
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
            lista.replaceChildren();
            const campos = [
                ["Impressora do perfil", corpo.modeloImpressora],
                ["Material configurado", corpo.material],
                ["Cor configurada", corpo.corMaterial],
                ["Altura de camada", corpo.alturaCamada ? `${corpo.alturaCamada} mm` : ""],
                ["Diâmetro do bico", corpo.diametroBico ? `${corpo.diametroBico} mm` : ""],
                ["Perfil de impressão", corpo.perfil],
                ["Aplicativo", corpo.fatiador]
            ];
            campos.forEach(([rotulo, valor]) => {
                if (!valor) return;
                const termo = document.createElement("dt");
                termo.textContent = rotulo;
                const descricao = document.createElement("dd");
                if (rotulo === "Cor configurada") {
                    const cores = valor.split(",").map(cor => cor.trim()).filter(cor => /^#(?:[\da-f]{3}|[\da-f]{6})$/i.test(cor));
                    if (cores.length) {
                        const amostras = document.createElement("span");
                        amostras.className = "importacao-peca-amostras-cor";
                        cores.forEach(cor => {
                            const amostra = document.createElement("span");
                            amostra.className = "importacao-peca-amostra-cor";
                            amostra.style.backgroundColor = cor;
                            amostra.title = cor;
                            amostra.setAttribute("aria-label", `Cor ${cor}`);
                            amostras.appendChild(amostra);
                        });
                        descricao.append(amostras, document.createTextNode(valor));
                    } else {
                        descricao.textContent = valor;
                    }
                } else {
                    descricao.textContent = valor;
                }
                lista.append(termo, descricao);
            });
            preview.hidden = true;
            preview.removeAttribute("src");
            layoutDados.classList.add("sem-imagem");
            if (typeof corpo.imagemPreviewDataUri === "string"
                    && /^data:image\/(?:png|jpeg|webp);base64,/i.test(corpo.imagemPreviewDataUri)) {
                preview.src = corpo.imagemPreviewDataUri;
                preview.hidden = false;
                layoutDados.classList.remove("sem-imagem");
            }
            avisoFatiamento.textContent = corpo.jaFatiado
                ? "O arquivo parece incluir dados de fatiamento. Nesta primeira etapa, o Criare importa o nome e as configurações; tempo e consumo ainda precisam ser conferidos e preenchidos manualmente."
                : "Este projeto ainda não foi fatiado. Ele não informa o tempo nem os gramas usados; esses campos continuam para preenchimento manual nesta etapa.";
            dados.hidden = false;
            informar(`Projeto lido. O nome “${corpo.nome}” foi colocado no cadastro; você pode editá-lo.`);
        } catch (erro) {
            informar(erro.message || "Não foi possível ler esse projeto 3MF.", true);
        } finally {
            botao.disabled = false;
        }
    });
}

document.addEventListener("DOMContentLoaded", inicializarImportadorModelo3mf);
document.addEventListener("turbo:load", inicializarImportadorModelo3mf);
