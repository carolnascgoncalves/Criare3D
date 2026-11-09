(() => {
    let logoUrlTemporario = null;

    function canalLinear(valor) {
        const canal = valor / 255;
        return canal <= 0.04045 ? canal / 12.92 : ((canal + 0.055) / 1.055) ** 2.4;
    }

    function luminancia(hex) {
        if (!/^#[\da-f]{6}$/i.test(hex || "")) return 0;
        return 0.2126 * canalLinear(parseInt(hex.slice(1, 3), 16))
            + 0.7152 * canalLinear(parseInt(hex.slice(3, 5), 16))
            + 0.0722 * canalLinear(parseInt(hex.slice(5, 7), 16));
    }

    function textoSobre(hex) {
        const l = luminancia(hex);
        const escuro = (l + 0.05) / 0.069;
        const claro = 1.05 / (l + 0.05);
        return escuro >= claro ? "#2b211d" : "#ffffff";
    }

    function atualizarPrevia(formulario) {
        const previa = document.getElementById("clientePreview");
        if (!previa) return;

        const corPrimaria = formulario.querySelector("#corPrimaria")?.value;
        const corDestaque = formulario.querySelector("#corDestaque")?.value;
        const corFundo = formulario.querySelector("#corFundo")?.value;
        const nome = formulario.querySelector("#nome")?.value.trim();
        if (/^#[\da-f]{6}$/i.test(corPrimaria || "")) {
            previa.style.setProperty("--cliente-cor-primaria", corPrimaria);
            previa.style.setProperty("--cliente-texto-primaria", textoSobre(corPrimaria));
        }
        if (/^#[\da-f]{6}$/i.test(corDestaque || "")) {
            previa.style.setProperty("--cliente-cor-destaque", corDestaque);
            previa.style.setProperty("--cliente-texto-destaque", textoSobre(corDestaque));
        }
        if (/^#[\da-f]{6}$/i.test(corFundo || "")) previa.style.setProperty("--cliente-cor-fundo", corFundo);

        const nomeVisivel = nome || "Nome do cliente";
        previa.querySelectorAll("[data-preview-nome], [data-preview-titulo]")
            .forEach(elemento => { elemento.textContent = nomeVisivel; });
    }

    function atualizarPreviaLogo(formulario) {
        const input = formulario.querySelector("[data-logo-input]");
        const imagem = formulario.querySelector("[data-logo-arquivo]");
        const existente = formulario.querySelector("[data-logo-existente]");
        const placeholder = formulario.querySelector("[data-logo-placeholder]");
        const arquivo = input?.files?.[0];
        if (!imagem) return;

        if (logoUrlTemporario) URL.revokeObjectURL(logoUrlTemporario);
        logoUrlTemporario = null;
        const remover = formulario.querySelector("[data-remover-logo]");
        if (arquivo && ["image/png", "image/jpeg"].includes(arquivo.type)) {
            logoUrlTemporario = URL.createObjectURL(arquivo);
            imagem.src = logoUrlTemporario;
            imagem.hidden = false;
            if (existente) existente.hidden = true;
            if (placeholder) placeholder.hidden = true;
        } else {
            imagem.hidden = true;
            const logoRemovido = Boolean(remover?.checked);
            if (existente) existente.hidden = logoRemovido;
            if (placeholder) placeholder.hidden = Boolean(existente) && !logoRemovido;
        }
    }

    function iniciarFormularioCliente() {
        const formulario = document.querySelector(".cliente-formulario");
        if (!formulario || formulario.dataset.inicializado === "true") return;
        formulario.dataset.inicializado = "true";
        formulario.addEventListener("input", evento => {
            if (evento.target.matches("#nome, [data-cor-tema]")) atualizarPrevia(formulario);
        });
        formulario.querySelector("[data-logo-input]")?.addEventListener("change", () => {
            const remover = formulario.querySelector("[data-remover-logo]");
            if (remover?.checked) remover.checked = false;
            atualizarPreviaLogo(formulario);
        });
        formulario.querySelector("[data-remover-logo]")?.addEventListener("change", () => atualizarPreviaLogo(formulario));
        atualizarPrevia(formulario);
    }

    document.addEventListener("click", async evento => {
        const botao = evento.target.closest("[data-copiar-senha]");
        if (!botao) return;
        const texto = botao.closest(".cliente-senha-temporaria")?.querySelector("code")?.textContent?.trim();
        if (!texto) return;
        try {
            await navigator.clipboard.writeText(texto);
            botao.innerHTML = '<i class="bi bi-check2"></i> Copiada';
        } catch (_) {
            botao.title = "Não foi possível copiar automaticamente. Selecione e copie a senha.";
        }
    });

    document.addEventListener("input", evento => {
        const campo = evento.target.closest("[data-nome-esperado]");
        if (!campo) return;
        const botao = campo.form?.querySelector("[data-confirmar-exclusao]");
        if (botao) botao.disabled = campo.value.trim() !== campo.dataset.nomeEsperado;
    });

    document.addEventListener("turbo:load", iniciarFormularioCliente);
    document.addEventListener("DOMContentLoaded", iniciarFormularioCliente, { once: true });
    document.addEventListener("turbo:before-cache", () => {
        document.querySelectorAll("[data-senha-temporaria]").forEach(aviso => aviso.remove());
    });
})();
