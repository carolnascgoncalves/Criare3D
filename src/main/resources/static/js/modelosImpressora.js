(() => {
    const camposInicializados = new WeakSet();
    let modelos = [];
    let carregamento;
    function carregarModelos() {
        if (!carregamento) {
            carregamento = fetch('/catalogo-opcoes/modelos-impressora', {headers: {Accept: 'application/json'}})
                .then(resposta => {
                    if (!resposta.ok) throw new Error('Não foi possível carregar os modelos de impressora.');
                    return resposta.json();
                })
                .then(dados => { modelos = dados; return modelos; })
                .catch(erro => { carregamento = null; throw erro; });
        }
        return carregamento;
    }
    window.carregarModelosImpressora = carregarModelos;
    window.estimarUsoImpressora = async (modelo, impressoraId) => {
        const parametros = new URLSearchParams({modelo});
        if (impressoraId) parametros.set('impressoraId', impressoraId);
        const resposta = await fetch('/catalogo-opcoes/impressora/estimativa-uso?' + parametros,
            {headers: {Accept: 'application/json'}, credentials: 'same-origin'});
        if (!resposta.ok || resposta.redirected) throw new Error('Não foi possível carregar as estimativas de uso.');
        return resposta.json();
    };

    async function inicializarModelos() {
        if (!document.querySelectorAll("#modelo[list='modelosBambu']").length) return;
        try { await carregarModelos(); } catch (erro) {
            const aviso = document.getElementById('referenciaPotenciaFabricante');
            if (aviso) { aviso.textContent = erro.message; aviso.hidden = false; }
            return;
        }
        document.querySelectorAll("#modelo[list='modelosBambu']").forEach(campo => {
            if (camposInicializados.has(campo)) return;
            camposInicializados.add(campo);
            const aviso = document.getElementById("referenciaPotenciaFabricante");
            const campoPotencia = document.getElementById("potenciaWatts");
            const dadosTecnicos = document.getElementById("dadosTecnicosImpressora");
            let preenchendoPotencia = false;

            campoPotencia?.addEventListener("input", () => {
                if (!preenchendoPotencia) delete campoPotencia.dataset.valorCatalogo;
            });

            campo.addEventListener("input", () => {
                if (dadosTecnicos) dadosTecnicos.hidden = !campo.value.trim();
                if (aviso) aviso.replaceChildren();
                const modelo = modelos.find(item => item.nome.toLocaleLowerCase("pt-BR") === campo.value.trim().toLocaleLowerCase("pt-BR"));
                const valorCatalogoAnterior = campoPotencia?.dataset.valorCatalogo;
                const valorAtualEhReferencia = modelos.some(item => item.potenciaMediaPLA
                    && Number(item.potenciaMediaPLA) === Number(campoPotencia?.value));
                if (campoPotencia && valorCatalogoAnterior && campoPotencia.value === valorCatalogoAnterior) {
                    campoPotencia.value = "";
                    delete campoPotencia.dataset.valorCatalogo;
                }

                if (modelo?.potenciaMediaPLA && campoPotencia
                    && (!campoPotencia.value || valorCatalogoAnterior || valorAtualEhReferencia)) {
                    preenchendoPotencia = true;
                    campoPotencia.value = String(modelo.potenciaMediaPLA);
                    campoPotencia.dataset.valorCatalogo = String(modelo.potenciaMediaPLA);
                    campoPotencia.dispatchEvent(new Event("input", {bubbles: true}));
                    preenchendoPotencia = false;
                }

                if (!aviso) return;
                if (modelo?.potenciaMaxima) {
                    const outrasMedias = Object.entries(modelo.potenciasOutrosMateriais || {})
                        .map(([material, watts]) => `${material} ${watts} W`).join(" · ");
                    const texto = modelo.potenciaMediaPLA
                        ? `${modelo.nome}: médias de referência de impressão: PLA ${modelo.potenciaMediaPLA} W${outrasMedias ? ` · ${outrasMedias}` : ""}. Potência máxima: ${modelo.potenciaMaxima}. O campo usa PLA como ponto de partida; o consumo varia por material e condições de teste.`
                        : `${modelo.nome}: o fabricante informa potência máxima de ${modelo.potenciaMaxima}. Esse limite não é a média usada no cálculo; informe uma média medida ou estimada.`;
                    aviso.append(document.createTextNode(texto));
                    if (modelo.potenciaMediaPLA && campoPotencia
                        && Number(campoPotencia.value) !== modelo.potenciaMediaPLA) {
                        const usarReferencia = document.createElement("button");
                        usarReferencia.type = "button";
                        usarReferencia.className = "botao botao-secundario botao-pequeno referencia-modelo-aplicar";
                        usarReferencia.textContent = `Usar referência PLA: ${modelo.potenciaMediaPLA} W`;
                        usarReferencia.addEventListener("click", () => {
                            preenchendoPotencia = true;
                            campoPotencia.value = String(modelo.potenciaMediaPLA);
                            campoPotencia.dataset.valorCatalogo = String(modelo.potenciaMediaPLA);
                            campoPotencia.dispatchEvent(new Event("input", {bubbles: true}));
                            campoPotencia.dispatchEvent(new Event("change", {bubbles: true}));
                            preenchendoPotencia = false;
                            campo.dispatchEvent(new Event("input"));
                        });
                        aviso.append(" ", usarReferencia);
                    }
                    if (modelo.fonte) {
                        const link = document.createElement("a");
                        link.href = modelo.fonte;
                        link.target = "_blank";
                        link.rel = "noopener noreferrer";
                        link.textContent = "Ver especificação oficial";
                        aviso.append(" ", link);
                    }
                    aviso.hidden = false;
                } else {
                    aviso.hidden = true;
                }
            });
            // Datalist/selectores de modelo podem disparar change sem um novo input.
            campo.addEventListener("change", () => campo.dispatchEvent(new Event("input", {bubbles: true})));
            campo.dispatchEvent(new Event("input"));
        });
    }

    document.addEventListener("DOMContentLoaded", inicializarModelos);
    document.addEventListener("turbo:load", inicializarModelos);
})();
