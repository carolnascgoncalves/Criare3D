
function atualizarCampoMarkup() {
    const markupFixo = document.getElementById("markupFixo");
    const campoMarkup = document.getElementById("campoMarkup");
    const markupPercentual = document.getElementById("markupPercentual");
    const avisoMarkupVariavel = document.getElementById("avisoMarkupVariavel");

    if (!markupFixo || !campoMarkup || !markupPercentual || !avisoMarkupVariavel) {
        return;
    }

    const ativo = markupFixo.checked;

    campoMarkup.hidden = !ativo;
    campoMarkup.classList.toggle("campo-oculto", !ativo);

    avisoMarkupVariavel.hidden = ativo;
    markupPercentual.required = ativo;
}

function atualizarCampoEmbalagem() {
    const embalagemFixa = document.getElementById("embalagemFixa");
    const campoCustoEmbalagem = document.getElementById("campoCustoEmbalagem");
    const custoEmbalagem = document.getElementById("custoEmbalagemFixa");

    if (!embalagemFixa || !campoCustoEmbalagem || !custoEmbalagem) {
        return;
    }

    const ativo = embalagemFixa.checked;

    campoCustoEmbalagem.hidden = !ativo;
    campoCustoEmbalagem.classList.toggle("campo-oculto", !ativo);

    custoEmbalagem.required = ativo;
}

function inicializarConfiguracoes() {
    atualizarCampoMarkup();
    atualizarCampoEmbalagem();
}

document.addEventListener("DOMContentLoaded", inicializarConfiguracoes);
document.addEventListener("turbo:load", inicializarConfiguracoes);

document.addEventListener("change", event => {
    if (event.target.id === "markupFixo") {
        atualizarCampoMarkup();
    }

    if (event.target.id === "embalagemFixa") {
        atualizarCampoEmbalagem();
    }
});
