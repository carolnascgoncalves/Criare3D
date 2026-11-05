let formularioFreteMercadoLivreInicializado = null;

function inicializarFreteMercadoLivre() {
    const formulario = document.getElementById("formCalculo");
    const blocoAutomatico = document.getElementById("freteMercadoLivreAutomatico");
    const blocoManual = document.getElementById("freteMercadoLivreManualBloco");
    const flagManual = document.getElementById("freteMercadoLivreManualNecessario");

    if (!formulario || !blocoAutomatico || !blocoManual || !flagManual) return;
    if (formulario === formularioFreteMercadoLivreInicializado) return;

    formularioFreteMercadoLivreInicializado = formulario;

    const campoManual = document.getElementById("freteMercadoLivreManual");
    const pesoPacote = document.getElementById("faixaPesoPacote");
    const campoPlataforma = document.getElementById("plataformaVenda");

    function arredondarMoeda(valor) {
        return Math.round((valor + Number.EPSILON) * 100) / 100;
    }

    function numero(valor) {
        if (valor == null || valor === "") return null;
        const convertido = Number(String(valor).replace(",", "."));
        return Number.isFinite(convertido) ? convertido : null;
    }

    function obterMarkup() {
        if (formulario.dataset.markupFixo === "true") {
            return numero(formulario.dataset.markupConfigurado);
        }

        const selecionado = formulario.querySelector(
            'input[name="markupPercentual"]:checked'
        );

        if (selecionado) return numero(selecionado.value);

        return numero(formulario.querySelector('[name="markupPercentual"]')?.value);
    }

    function calcularCustoBase() {
        const itens = [
            ...formulario.querySelectorAll("#listaFilamentos [data-filamento-item]")
        ];

        if (itens.length === 0) return null;

        let custoFilamento = 0;

        for (const item of itens) {
            const select = item.querySelector("[data-campo-filamento]");
            const quantidade = numero(item.querySelector("[data-campo-quantidade]")?.value);
            const opcao = select?.selectedOptions[0];
            const precoRolo = numero(opcao?.dataset.precoRolo);
            const pesoRolo = numero(opcao?.dataset.pesoRolo);

            if (!opcao?.value || quantidade == null || precoRolo == null || !pesoRolo) {
                return null;
            }

            custoFilamento += arredondarMoeda(quantidade * precoRolo / pesoRolo);
        }

        custoFilamento = arredondarMoeda(custoFilamento);

        const impressora = document.getElementById("impressoraId")
            ?.selectedOptions[0];
        const potencia = numero(impressora?.dataset.potencia);
        const minutos = numero(document.getElementById("tempoImpressaoMinutos")?.value);
        const tarifaEnergia = numero(formulario.dataset.tarifaEnergia);

        if (potencia == null || minutos == null || tarifaEnergia == null) return null;

        const custoEnergia = arredondarMoeda(
            potencia / 1000 * minutos / 60 * tarifaEnergia
        );

        let custoEmbalagem;

        if (formulario.dataset.embalagemFixa === "true") {
            custoEmbalagem = numero(formulario.dataset.custoEmbalagemFixa);
        } else {
            const embalagem = document.getElementById("embalagemId")
                ?.selectedOptions[0];
            custoEmbalagem = numero(embalagem?.dataset.custo);
        }

        const markup = obterMarkup();

        if (custoEmbalagem == null || markup == null || markup < 0) return null;

        const custoTotal = arredondarMoeda(
            custoFilamento + custoEnergia + arredondarMoeda(custoEmbalagem)
        );

        return arredondarMoeda(custoTotal * (1 + markup / 100));
    }

    function obterFreteAutomatico(peso, preco) {
        let coluna;

        if (preco <= 48.99) coluna = 0;
        else if (preco <= 78.99) coluna = 1;
        else if (preco <= 99.99) coluna = 2;
        else if (preco <= 119.99) coluna = 3;
        else return null;

        const linhas = {
            ATE_300G: [6.85, 8.15, 12.95, 14.95],
            DE_300G_500G: [6.95, 8.25, 13.85, 16.15],
            DE_500G_1KG: [7.15, 8.45, 14.45, 16.85],
            DE_1KG_1_5KG: [7.35, 8.65, 14.75, 17.15],
            DE_1_5KG_2KG: [7.45, 8.75, 15.05, 17.65],
            DE_2KG_3KG: [8.65, 9.15, 16.45, 19.15],
            DE_3KG_4KG: [8.75, 9.75, 17.85, 20.75],
            DE_4KG_5KG: [8.85, 10.25, 19.75, 22.85]
        };

        return linhas[peso]?.[coluna] ?? null;
    }

    function calcularPrecisaFreteManual() {
        const base = calcularCustoBase();
        const peso = pesoPacote?.value;
        const anuncio = document.getElementById("tipoAnuncio")?.value;

        if (base == null || !anuncio) return null;

        const comissao = anuncio === "PREMIUM" ? 19 : 14;
        const percentualLiquido = 1 - comissao / 100;

        let preco = arredondarMoeda(base / percentualLiquido);

        for (let tentativa = 0; tentativa < 5; tentativa++) {
            if (preco > 119.99) return true;

            const frete = obterFreteAutomatico(peso, preco);
            if (frete == null) return peso ? true : null;

            const novoPreco = arredondarMoeda((base + frete) / percentualLiquido);

            if (novoPreco === preco) return false;
            preco = novoPreco;
        }

        return preco > 119.99;
    }

    function atualizarBlocoFrete() {
        if (campoPlataforma?.value !== "MERCADO_LIVRE") return;

        const estimativa = calcularPrecisaFreteManual();
        const precisaManual = estimativa == null
            ? flagManual.value === "true"
            : estimativa;

        flagManual.value = String(precisaManual);
        blocoAutomatico.hidden = precisaManual;
        blocoManual.hidden = !precisaManual;

        if (pesoPacote) pesoPacote.disabled = precisaManual;

        if (campoManual) {
            campoManual.disabled = !precisaManual;
            campoManual.required = precisaManual;
        }
    }

    formulario.addEventListener("input", atualizarBlocoFrete);
    formulario.addEventListener("change", atualizarBlocoFrete);
    atualizarBlocoFrete();
}

document.addEventListener("DOMContentLoaded", inicializarFreteMercadoLivre);
document.addEventListener("turbo:load", inicializarFreteMercadoLivre);
