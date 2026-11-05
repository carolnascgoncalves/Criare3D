(() => {
    function obterGrupoRadio(campo) {
        return campo.type === "radio"
            ? campo.closest(".markup-opcoes")
            : null;
    }

    function obterCaixaExistente(campo) {
        const grupoRadio = obterGrupoRadio(campo);

        if (grupoRadio) {
            const proximo = grupoRadio.nextElementSibling;

            return proximo?.classList.contains("erro-campo")
                ? proximo
                : null;
        }

        const grupo = campo.closest(".campo") || campo.parentElement;
        return grupo?.querySelector(".erro-campo") || null;
    }

    function obterCaixaErro(campo) {
        const existente = obterCaixaExistente(campo);

        if (existente) return existente;

        const caixa = document.createElement("div");
        caixa.className = "erro-campo";

        const grupoRadio = obterGrupoRadio(campo);

        if (grupoRadio) {
            grupoRadio.insertAdjacentElement("afterend", caixa);
            return caixa;
        }

        campo.insertAdjacentElement("afterend", caixa);
        return caixa;
    }

    function formatarLimite(valor) {
        return String(valor).replace(".", ",");
    }

    function obterMensagem(campo) {
        const validade = campo.validity;

        if (validade.valueMissing) {
            return campo.dataset.mensagemObrigatoria || "Preencha este campo.";
        }

        if (validade.typeMismatch && campo.type === "email") {
            return "Informe um e-mail válido.";
        }

        if (validade.typeMismatch) {
            return "Informe um valor em formato válido.";
        }

        if (validade.tooShort) {
            return `Informe pelo menos ${campo.minLength} caracteres.`;
        }

        if (validade.tooLong) {
            return `Informe no máximo ${campo.maxLength} caracteres.`;
        }

        if (validade.rangeUnderflow) {
            return `Informe um valor igual ou maior que ${formatarLimite(campo.min)}.`;
        }

        if (validade.rangeOverflow) {
            return `Informe um valor igual ou menor que ${formatarLimite(campo.max)}.`;
        }

        if (validade.stepMismatch) {
            return "Informe um valor válido para este campo.";
        }

        if (validade.badInput) {
            return "Informe um número válido.";
        }

        if (validade.patternMismatch) {
            return campo.dataset.mensagemFormato || "Confira o formato informado.";
        }

        return "Confira o valor informado.";
    }

    function limpar(campo) {
        const caixa = obterCaixaExistente(campo);

        if (caixa) {
            caixa.textContent = "";
            caixa.hidden = true;
        }

        campo.removeAttribute("aria-invalid");
    }

    function validar(campo) {
        if (!campo || campo.disabled || campo.type === "hidden") {
            return true;
        }

        if (campo.closest("[hidden]")) {
            return true;
        }

        if (campo.validity.valid) {
            limpar(campo);
            return true;
        }

        const caixa = obterCaixaErro(campo);

        caixa.textContent = obterMensagem(campo);
        caixa.hidden = false;
        campo.setAttribute("aria-invalid", "true");

        return false;
    }

    function atualizarErroAoEditar(evento) {
        const campo = evento.target;

        if (!(campo instanceof HTMLInputElement
            || campo instanceof HTMLSelectElement
            || campo instanceof HTMLTextAreaElement)) {
            return;
        }

        const caixa = obterCaixaExistente(campo);

        if (caixa?.textContent.trim()) {
            validar(campo);
        }
    }

    document.addEventListener("input", atualizarErroAoEditar);
    document.addEventListener("change", atualizarErroAoEditar);

    window.validacaoInline = {
        validar,
        limpar
    };
})();