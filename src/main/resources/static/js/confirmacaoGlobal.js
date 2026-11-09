(() => {
    let configurado = false;

    function confirmarAcao(mensagem, elemento) {
        let dialogo = document.getElementById("confirmacaoGlobal");
        if (!dialogo) {
            dialogo = document.createElement("dialog");
            dialogo.id = "confirmacaoGlobal";
            dialogo.className = "confirmacao-global";
            dialogo.innerHTML = `
                <form method="dialog" class="confirmacao-global-conteudo">
                    <span class="confirmacao-global-icone" aria-hidden="true"><i class="bi bi-exclamation-triangle"></i></span>
                    <h2 data-confirmacao-titulo>Confirmar ação</h2>
                    <p data-confirmacao-mensagem></p>
                    <div class="confirmacao-global-acoes">
                        <button type="submit" value="cancelar" class="botao botao-secundario" data-confirmacao-cancelar>Cancelar</button>
                        <button type="submit" value="confirmar" class="botao botao-perigo-suave" data-confirmacao-aceitar>Confirmar</button>
                    </div>
                </form>`;
        }
        if (!dialogo.isConnected) document.body.append(dialogo);

        dialogo.querySelector("[data-confirmacao-titulo]").textContent =
            elemento?.dataset.confirmTitle || "Confirmar ação";
        dialogo.querySelector("[data-confirmacao-mensagem]").textContent = mensagem || "Deseja continuar com esta ação?";
        dialogo.querySelector("[data-confirmacao-aceitar]").textContent =
            elemento?.dataset.confirmLabel || "Confirmar";

        return new Promise(resolve => {
            const finalizar = () => {
                dialogo.removeEventListener("close", aoFechar);
                resolve(dialogo.returnValue === "confirmar");
            };
            const aoFechar = () => finalizar();
            dialogo.addEventListener("close", aoFechar, { once: true });
            dialogo.showModal();
            dialogo.querySelector("[data-confirmacao-cancelar]").focus();
        });
    }

    function configurarTurbo() {
        if (configurado || !window.Turbo?.setConfirmMethod) return;
        window.Turbo.setConfirmMethod(confirmarAcao);
        configurado = true;
    }

    window.confirmarAcao = confirmarAcao;
    document.addEventListener("DOMContentLoaded", configurarTurbo, { once: true });
    document.addEventListener("turbo:load", configurarTurbo);
})();
