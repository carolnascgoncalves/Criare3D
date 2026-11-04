document.addEventListener("turbo:load", () => {
    const exemplos = {
        "Nome da peça": "Ex.: Dragão articulado",
        "Nome da impressora": "Ex.: Impressora principal",
        "Modelo": "Ex.: Bambu Lab A1",
        "Nome do filamento": "Ex.: PLA Premium",
        "Nome da cor": "Ex.: Rosa bebê",
        "Marca": "Ex.: Bambu Lab",
        "Nome da embalagem": "Ex.: Caixa pequena",
        "Quantidade de filamento (gramas)": "Ex.: 120.00",
        "Tempo de impressão (minutos)": "Ex.: 180",
        "Potência (W)": "Ex.: 350",
        "Peso do rolo (gramas)": "Ex.: 1000",
        "Preço do rolo (R$)": "Ex.: 89.90",
        "Custo unitário (R$)": "Ex.: 2.50"
    };

    document.querySelectorAll(".campo input").forEach(input => {
        if (input.hasAttribute("placeholder")) return;

        if (["checkbox", "radio", "color", "file", "hidden"].includes(input.type)) {
            return;
        }

        const campo = input.closest(".campo");
        const rotulo = campo?.querySelector("label")?.textContent.trim().replace(/\s+/g, " ");
        const exemplo = exemplos[rotulo];

        if (exemplo) {
            input.setAttribute("placeholder", exemplo);
        }
    });
});