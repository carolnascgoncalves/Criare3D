document.addEventListener("turbo:load", () => {
    const markupFixo = document.getElementById("markupFixo");
    const campoMarkup = document.getElementById("campoMarkup");
    const markupPercentual = document.getElementById("markupPercentual");
    const avisoMarkupVariavel = document.getElementById("avisoMarkupVariavel");

    const embalagemFixa = document.getElementById("embalagemFixa");
    const campoCustoEmbalagem = document.getElementById("campoCustoEmbalagem");
    const custoEmbalagem = document.getElementById("custoEmbalagemFixa");

    if (
        !markupFixo ||
        !campoMarkup ||
        !markupPercentual ||
        !avisoMarkupVariavel ||
        !embalagemFixa ||
        !campoCustoEmbalagem ||
        !custoEmbalagem
    ) {
        return;
    }

    function atualizarMarkup() {
        campoMarkup.hidden = !markupFixo.checked;
        avisoMarkupVariavel.hidden = markupFixo.checked;
        markupPercentual.required = markupFixo.checked;
    }

    function atualizarEmbalagem() {
        campoCustoEmbalagem.hidden = !embalagemFixa.checked;
        custoEmbalagem.required = embalagemFixa.checked;
    }

    markupFixo.addEventListener("change", atualizarMarkup);
    embalagemFixa.addEventListener("change", atualizarEmbalagem);

    atualizarMarkup();
    atualizarEmbalagem();
});