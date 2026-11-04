
function inicializarConfiguracoes() {
    const markupFixo = document.getElementById("markupFixo");
    const campoMarkup = document.getElementById("campoMarkup");
    const markupPercentual = document.getElementById("markupPercentual");
    const avisoMarkupVariavel = document.getElementById("avisoMarkupVariavel");

    const embalagemFixa = document.getElementById("embalagemFixa");
    const campoCustoEmbalagem = document.getElementById("campoCustoEmbalagem");
    const custoEmbalagem = document.getElementById("custoEmbalagemFixa");

    if (markupFixo && campoMarkup && markupPercentual && avisoMarkupVariavel) {
        function atualizarMarkup() {
            const ativo = markupFixo.checked;

            campoMarkup.hidden = !ativo;
            campoMarkup.classList.toggle("campo-oculto", !ativo);

            avisoMarkupVariavel.hidden = ativo;
            markupPercentual.required = ativo;
        }

        markupFixo.addEventListener("change", atualizarMarkup);
        atualizarMarkup();
    }

    if (embalagemFixa && campoCustoEmbalagem && custoEmbalagem) {
        function atualizarEmbalagem() {
            const ativo = embalagemFixa.checked;

            campoCustoEmbalagem.hidden = !ativo;
            campoCustoEmbalagem.classList.toggle("campo-oculto", !ativo);

            custoEmbalagem.required = ativo;
        }

        embalagemFixa.addEventListener("change", atualizarEmbalagem);
        atualizarEmbalagem();
    }
}

document.addEventListener("DOMContentLoaded", inicializarConfiguracoes);
document.addEventListener("turbo:load", inicializarConfiguracoes);
