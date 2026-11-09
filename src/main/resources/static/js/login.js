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
