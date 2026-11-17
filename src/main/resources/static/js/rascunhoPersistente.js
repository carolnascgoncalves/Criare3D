(() => {
    if (window.filaRascunhoCalculo) return;
    const pendentes = new Map();
    const enviados = new Map();
    let fila = Promise.resolve();
    let timer;

    function status(id, mensagem) {
        if (document.getElementById('rascunhoCalculoId')?.value !== id) return;
        const campo = document.querySelector('[data-rascunho-status]');
        if (campo) campo.textContent = mensagem;
    }

    function sincronizar() {
        clearTimeout(timer);
        const trabalhos = [...pendentes];
        pendentes.clear();
        fila = fila.catch(() => {}).then(async () => {
            for (const [id, trabalho] of trabalhos) {
                if (enviados.get(id) === trabalho.assinatura) continue;
                status(id, 'Salvando rascunho…');
                try {
                    const tokens = document.querySelectorAll('meta[name="csrf-token"]');
                    const resposta = await fetch('/historico/rascunho?rascunhoId=' + encodeURIComponent(id), {
                        method: 'POST', credentials: 'same-origin', keepalive: true,
                        headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': tokens[tokens.length - 1]?.content || ''},
                        body: JSON.stringify(trabalho.dados)
                    });
                    if (!resposta.ok || resposta.redirected) throw new Error('Não foi possível salvar o rascunho no servidor.');
                    enviados.set(id, trabalho.assinatura);
                    status(id, 'Rascunho salvo.');
                } catch (erro) {
                    if (!pendentes.has(id)) pendentes.set(id, trabalho);
                    status(id, 'Não foi possível salvar no servidor. Uma cópia permanece neste navegador.');
                    throw erro;
                }
            }
        });
        return fila;
    }

    window.filaRascunhoCalculo = {
        agendar(id, dados) {
            if (!id) return;
            const assinatura = JSON.stringify({campos: dados.campos, filamentos: dados.filamentos, etapa: dados.etapa});
            if (enviados.get(id) === assinatura && !pendentes.has(id)) return;
            pendentes.set(id, {dados, assinatura});
            clearTimeout(timer);
            timer = setTimeout(() => sincronizar().catch(() => {}), 600);
        },
        sincronizar,
        descartar(id) { pendentes.delete(id); enviados.delete(id); }
    };
})();
