(() => {
    if (window.__compradoresInicializado) return;
    window.__compradoresInicializado = true;

    const dialogo = () => document.getElementById('dialogo-cliente');

    function preencher(dados) {
        const d = dialogo();
        if (!d) return;
        const form = d.querySelector('form');
        const novo = !dados;
        const valores = dados || {};
        form.querySelector('[name=id]').value = valores.id || '';
        form.querySelector('[name=versao]').value = valores.versao || '';
        form.querySelector('[name=nome]').value = valores.nome || '';
        form.querySelector('[name=telefone]').value = valores.telefone || '';
        form.querySelector('[name=email]').value = valores.email || '';
        form.querySelector('[name=observacoes]').value = valores.observacoes || '';
        form.querySelectorAll('.erro-campo').forEach((erro) => { erro.textContent = ''; });
        const titulo = d.querySelector('[data-cliente-titulo]');
        if (titulo) titulo.textContent = novo ? 'Novo cliente' : 'Editar cliente';
    }

    function abrir() {
        const d = dialogo();
        if (!d || d.open) return;
        d.showModal();
        d.querySelector('[name=nome]')?.focus();
    }

    function abrirSeSolicitado() {
        const d = dialogo();
        if (d && d.dataset.abrir === 'true' && !d.open) d.showModal();
    }

    document.addEventListener('click', (evento) => {
        const alvo = evento.target;
        if (!(alvo instanceof Element)) return;

        if (alvo.closest('[data-cliente-novo]')) {
            evento.preventDefault();
            preencher(null);
            abrir();
            return;
        }
        const editar = alvo.closest('[data-cliente-editar]');
        if (editar) {
            evento.preventDefault();
            editar.closest('details')?.removeAttribute('open');
            preencher({ ...editar.dataset });
            abrir();
            return;
        }
        if (alvo.closest('[data-cliente-fechar]')) {
            dialogo()?.close();
            return;
        }
        if (alvo === dialogo()) {
            dialogo().close();
            return;
        }
        document.querySelectorAll('.menu-acoes[open]').forEach((menu) => {
            if (!menu.contains(alvo)) menu.removeAttribute('open');
        });
    });

    document.addEventListener('turbo:before-cache', () => dialogo()?.close());
    document.addEventListener('DOMContentLoaded', abrirSeSolicitado);
    document.addEventListener('turbo:load', abrirSeSolicitado);
})();
