document.addEventListener("DOMContentLoaded", () => {
    const plataforma = document.getElementById("plataformaVenda");

    const infoShopee = document.getElementById("infoShopee");
    const infoTikTok = document.getElementById("infoTikTok");
    const infoMercadoLivre = document.getElementById("infoMercadoLivre");
    const extras = document.getElementById("plataformaExtras");

    const tipoAnuncio = document.getElementById("tipoAnuncio");
    const percentualComissao = document.getElementById("percentualComissaoPlataforma");

    if (!plataforma) return;

    function atualizarPlataforma() {
        const selecionada = plataforma.value;

        infoShopee.hidden = selecionada !== "SHOPEE";
        infoTikTok.hidden = selecionada !== "TIKTOK";
        infoMercadoLivre.hidden = selecionada !== "MERCADO_LIVRE";

        extras.hidden = selecionada === "NENHUMA";

        if (selecionada === "MERCADO_LIVRE") {
            atualizarTipoAnuncio();
        } else {
            percentualComissao.required = false;
        }
    }

    function atualizarTipoAnuncio() {
        if (!tipoAnuncio || !percentualComissao) return;

        const premium = tipoAnuncio.value === "PREMIUM";

        percentualComissao.min = premium ? "15" : "10";
        percentualComissao.max = premium ? "19" : "14";
        percentualComissao.required = true;

        percentualComissao.placeholder = premium
            ? "Ex.: 16"
            : "Ex.: 12";
    }

    plataforma.addEventListener("change", atualizarPlataforma);
    tipoAnuncio?.addEventListener("change", atualizarTipoAnuncio);

    atualizarPlataforma();
});