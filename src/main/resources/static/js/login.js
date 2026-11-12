document.addEventListener("click", evento => {
    const botao = evento.target.closest("[data-alternar-senha]");
    if (!botao) return;
    const campo = document.getElementById("password");
    if (!campo) return;
    const mostrar = campo.type === "password";
    campo.type = mostrar ? "text" : "password";
    botao.setAttribute("aria-label", mostrar ? "Ocultar senha" : "Mostrar senha");
    botao.setAttribute("aria-pressed", String(mostrar));
    botao.innerHTML = `<i class="bi ${mostrar ? "bi-eye-slash" : "bi-eye"}" aria-hidden="true"></i>`;
});


(() => {
    let intervalo;
    function prepararEspera() {
        clearInterval(intervalo);
        const aviso = document.querySelector('[data-login-espera]');
        if (!aviso) return;
        const botao = document.querySelector('.login-formulario button[type="submit"]');
        const segundos = aviso.querySelector('[data-login-segundos]');
        const fim = Date.now() + Number(aviso.dataset.loginEspera) * 1000;
        function atualizar() {
            const restante = Math.max(0, Math.ceil((fim - Date.now()) / 1000));
            if (botao) botao.disabled = restante > 0;
            if (restante === 0) {
                clearInterval(intervalo);
                aviso.textContent = 'Você já pode tentar entrar novamente.';
            } else if (segundos) segundos.textContent = String(restante);
        }
        atualizar();
        intervalo = setInterval(atualizar, 250);
    }
    document.addEventListener('DOMContentLoaded', prepararEspera);
    document.addEventListener('turbo:load', prepararEspera);
    document.addEventListener('turbo:before-cache', () => clearInterval(intervalo));
})();
