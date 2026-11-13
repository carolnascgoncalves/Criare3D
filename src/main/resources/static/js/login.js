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

// A tipografia de apoio continua legível enquanto a imagem original carrega.
(() => {
    function carregarMarca() {
        document.querySelectorAll('[data-marca-imagem]').forEach(bloco => {
            const imagem = bloco.querySelector('img');
            const fallback = bloco.querySelector('[data-marca-fallback]');
            function mostrar() {
                if (!imagem.naturalWidth) return;
                imagem.hidden = false;
                if (fallback) fallback.hidden = true;
            }
            imagem.addEventListener('load', mostrar, {once: true});
            if (imagem.complete) mostrar();
        });
    }
    document.addEventListener('DOMContentLoaded', carregarMarca);
    document.addEventListener('turbo:load', carregarMarca);
})();

// Somente a confirmação de logout desaparece; erros e bloqueios continuam visíveis.
(() => {
    const temporizadores = new Map();
    function limpar() {
        temporizadores.forEach(clearTimeout);
        temporizadores.clear();
    }
    function prepararAvisos() {
        limpar();
        document.querySelectorAll('[data-aviso-temporario]').forEach(aviso => {
            if (aviso.hidden) return;
            temporizadores.set(aviso, setTimeout(() => {
                aviso.hidden = true;
                temporizadores.delete(aviso);
            }, 7000));
        });
    }
    document.addEventListener('click', evento => {
        const botao = evento.target.closest('[data-fechar-aviso]');
        if (!botao) return;
        const aviso = botao.closest('[data-aviso-temporario]');
        if (!aviso) return;
        clearTimeout(temporizadores.get(aviso));
        temporizadores.delete(aviso);
        aviso.hidden = true;
    });
    document.addEventListener('DOMContentLoaded', prepararAvisos);
    document.addEventListener('turbo:load', prepararAvisos);
    document.addEventListener('turbo:before-cache', () => {
        limpar();
        document.querySelectorAll('[data-aviso-temporario]').forEach(aviso => { aviso.hidden = true; });
    });
})();
