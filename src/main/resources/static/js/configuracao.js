
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

function atualizarCustoOpcional(idCheckbox, idCampo, idsInputs) {
    const checkbox = document.getElementById(idCheckbox);
    const campo = document.getElementById(idCampo);

    if (!checkbox || !campo) {
        return;
    }

    const ativo = checkbox.checked;
    campo.hidden = !ativo;
    campo.classList.toggle("campo-oculto", !ativo);

    if (idCheckbox === "taxaFalhaAtivada") {
        const aviso = document.getElementById("avisoTaxaFalha");
        if (aviso) aviso.hidden = !ativo;
    }

    if (ativo && idCheckbox === "taxaFalhaAtivada") {
        const percentual = document.getElementById("percentualFalha");
        if (percentual && !percentual.value) percentual.value = "5";
    }

    idsInputs.forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            input.required = ativo;
        }
    });
}

function atualizarCustosAdicionais() {
    atualizarCustoOpcional("maoObraAtivada", "campoMaoObra", ["valorMaoObraHora"]);
    atualizarCustoOpcional("taxaFalhaAtivada", "campoPercentualFalha", ["percentualFalha"]);

    const maoObra = document.getElementById("maoObraAtivada");
    const tempoFixo = document.getElementById("maoObraTempoFixo");
    const opcaoTempoFixo = document.getElementById("opcaoMaoObraTempoFixo");
    const campoTempoFixo = document.getElementById("campoTempoMaoObraFixo");
    const inputTempoFixo = document.getElementById("tempoMaoObraFixoMinutos");
    if (maoObra && tempoFixo && opcaoTempoFixo && campoTempoFixo) {
        opcaoTempoFixo.hidden = !maoObra.checked;
        const mostrarTempoFixo = maoObra.checked && tempoFixo.checked;
        campoTempoFixo.hidden = !mostrarTempoFixo;
        campoTempoFixo.classList.toggle("campo-oculto", !mostrarTempoFixo);
        if (inputTempoFixo) inputTempoFixo.required = mostrarTempoFixo;
    }

    const depreciacao = document.getElementById("depreciacaoAtivada");
    const avisoDepreciacao = document.getElementById("avisoDepreciacao");
    if (depreciacao && avisoDepreciacao) {
        avisoDepreciacao.hidden = !depreciacao.checked;
    }
}

function inicializarConfiguracoes() {
    atualizarCampoMarkup();
    atualizarCampoEmbalagem();
    atualizarCustosAdicionais();
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

    if (["maoObraAtivada", "maoObraTempoFixo", "depreciacaoAtivada", "taxaFalhaAtivada"].includes(event.target.id)) {
        atualizarCustosAdicionais();
    }
});
