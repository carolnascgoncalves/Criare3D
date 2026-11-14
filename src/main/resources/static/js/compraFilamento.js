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
                const unidades=Number(formulario.querySelector('#unidadesCompra')?.value||1),preco=Number(formulario.querySelector('#precoRolo')?.value||0),custom=Number(formulario.querySelector('#gramasCompradas')?.value||0);
                const total=custom>0?custom:valor*unidades;const resumo=formulario.querySelector('[data-compra-resumo]');
                if(resumo)resumo.textContent=new Intl.NumberFormat('pt-BR').format(total)+' g serão adicionados · custo da compra: '+(valor>0?preco*total/valor:0).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
            };

            formulario.addEventListener("input", atualizar);
            atualizar();
        });
    }

    document.addEventListener("DOMContentLoaded", inicializarCompraFilamento);
    document.addEventListener("turbo:load", inicializarCompraFilamento);
})();
