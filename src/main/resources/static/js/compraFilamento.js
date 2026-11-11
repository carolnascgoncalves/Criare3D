(() => {
    function inicializarCompraFilamento() {
        document.querySelectorAll(".filamento-compra-formulario, .pagina-compra-filamento form").forEach(formulario => {
            if (formulario.dataset.compraFilamentoInicializada === "true") return;
            formulario.dataset.compraFilamentoInicializada = "true";

            const peso = formulario.querySelector("#pesoRoloGramas");
            const referencias = formulario.querySelectorAll("[data-compra-peso-referencia]");
            if (!peso || referencias.length === 0) return;

            const atualizar = () => {
                const valor = Number(peso.value);
                const texto = Number.isFinite(valor) && valor > 0
                    ? `${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(valor)} g`
                    : "o peso do rolo";
                referencias.forEach(referencia => { referencia.textContent = texto; });
            };

            peso.addEventListener("input", atualizar);
            atualizar();
        });
    }

    document.addEventListener("DOMContentLoaded", inicializarCompraFilamento);
    document.addEventListener("turbo:load", inicializarCompraFilamento);
})();
