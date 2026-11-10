(() => {
    const modelos = [
        {nome: "Bambu Lab A1 mini"},
        {nome: "Bambu Lab A1"},
        {nome: "Bambu Lab P1P"},
        {
            nome: "Bambu Lab P1S",
            potenciaMaxima: "1000 W em 220 V ou 350 W em 110 V",
            fonte: "https://cdn1.bambulab.com/documentation/quick-start-59b0cefdc0fc4/P1S/English%20version-Quick%20Start%20Guide%20for%20P1S.pdf"
        },
        {
            nome: "Bambu Lab X1 Carbon",
            potenciaMaxima: "1000 W em 220 V ou 350 W em 110 V",
            fonte: "https://public-cdn.bambulab.com/store/bambulab-X1-carbon-tech-specs.pdf?v=20250715085516"
        },
        {nome: "Bambu Lab X1E"},
        {nome: "Bambu Lab H2D"},
        {nome: "Bambu Lab H2S"}
    ];

    function inicializarModelos() {
        document.querySelectorAll("#modelo[list='modelosBambu']").forEach(campo => {
            if (campo.dataset.catalogoInicializado === "true") return;
            campo.dataset.catalogoInicializado = "true";
            const aviso = document.getElementById("referenciaPotenciaFabricante");
            campo.addEventListener("input", () => {
                if (!aviso) return;
                aviso.replaceChildren();
                const modelo = modelos.find(item => item.nome.toLocaleLowerCase("pt-BR") === campo.value.trim().toLocaleLowerCase("pt-BR"));
                if (modelo?.potenciaMaxima) {
                    aviso.append(document.createTextNode(`${modelo.nome}: o fabricante informa potência máxima de ${modelo.potenciaMaxima}. Esse limite não é o consumo médio; mantenha a potência média medida ou estimada no campo acima.`));
                    const link = document.createElement("a");
                    link.href = modelo.fonte;
                    link.target = "_blank";
                    link.rel = "noopener noreferrer";
                    link.textContent = "Ver especificação oficial";
                    aviso.append(" ", link);
                    aviso.hidden = false;
                } else {
                    aviso.hidden = true;
                }
            });
            campo.dispatchEvent(new Event("input"));
        });
    }

    document.addEventListener("DOMContentLoaded", inicializarModelos);
    document.addEventListener("turbo:load", inicializarModelos);
})();
