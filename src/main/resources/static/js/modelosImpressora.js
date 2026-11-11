(() => {
    const modelos = [
        {
            nome: "Bambu Lab A1 mini",
            potenciaMediaPLA: 80,
            potenciasOutrosMateriais: {PETG: 75},
            potenciaMaxima: "150 W",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab A1",
            potenciaMediaPLA: 95,
            potenciasOutrosMateriais: {ABS: 200, PC: 150},
            potenciaMaxima: "1300 W em 220 V ou 350 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab P1P",
            potenciaMediaPLA: 110,
            potenciasOutrosMateriais: {ABS: 170, PC: 160},
            potenciaMaxima: "1000 W em 220 V ou 350 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab P1S",
            potenciaMediaPLA: 105,
            potenciasOutrosMateriais: {ABS: 140, PC: 135},
            potenciaMaxima: "1000 W em 220 V ou 350 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab X1 Carbon",
            potenciaMediaPLA: 105,
            potenciasOutrosMateriais: {ABS: 150, PC: 135},
            potenciaMaxima: "1000 W em 220 V ou 350 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab X1E",
            potenciaMediaPLA: 185,
            potenciasOutrosMateriais: {ABS: 260, PC: 230},
            potenciaMaxima: "1350 W em 220 V ou 600 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab H2D",
            potenciaMediaPLA: 197,
            potenciasOutrosMateriais: {PETG: 150, PC: 395},
            potenciaMaxima: "2200 W em 220 V ou 1320 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab H2S",
            potenciaMediaPLA: 200,
            potenciasOutrosMateriais: {PETG: 180, PC: 330},
            potenciaMaxima: "1800 W em 220 V ou 1250 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab H2C",
            potenciaMediaPLA: 200,
            potenciasOutrosMateriais: {PETG: 193, PC: 294},
            potenciaMaxima: "1800 W em 220 V ou 1250 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab A2L",
            potenciaMediaPLA: 145,
            potenciaMaxima: "1000 W em 220 V ou 1000 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab P2S",
            potenciaMediaPLA: 200,
            potenciaMaxima: "1200 W em 220 V ou 1000 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab X2D",
            potenciaMediaPLA: 250,
            potenciasOutrosMateriais: {PC: 550},
            potenciaMaxima: "1600 W em 220 V ou 1100 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        },
        {
            nome: "Bambu Lab H3C",
            potenciaMediaPLA: 240,
            potenciasOutrosMateriais: {PC: 540},
            potenciaMaxima: "2250 W em 220 V ou 1850 W em 110 V",
            fonte: "https://wiki.bambulab.com/en/general/power-consumption"
        }
    ];

    function inicializarModelos() {
        document.querySelectorAll("#modelo[list='modelosBambu']").forEach(campo => {
            if (campo.dataset.catalogoInicializado === "true") return;
            campo.dataset.catalogoInicializado = "true";
            const aviso = document.getElementById("referenciaPotenciaFabricante");
            const campoPotencia = document.getElementById("potenciaWatts");
            const dadosTecnicos = document.getElementById("dadosTecnicosImpressora");
            let preenchendoPotencia = false;

            campoPotencia?.addEventListener("input", () => {
                if (!preenchendoPotencia) delete campoPotencia.dataset.valorCatalogo;
            });

            campo.addEventListener("input", () => {
                if (dadosTecnicos) dadosTecnicos.hidden = !campo.value.trim()
                    && !dadosTecnicos.querySelector(".erro-campo");
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
