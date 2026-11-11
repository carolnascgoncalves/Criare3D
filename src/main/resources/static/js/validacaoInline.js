(() => {
    const temporizadores = new WeakMap();
    const mensagensPendentes = new Set();
    let avisoAgendado = false;

    function avisar(mensagem = '') {
        if (mensagem) mensagensPendentes.add(mensagem);
        if (avisoAgendado) return;
        avisoAgendado = true;
        queueMicrotask(() => {
            const detalhe = [...mensagensPendentes][0];
            window.mostrarNotificacao?.(`Não foi possível prosseguir. ${detalhe ? detalhe + ' ' : ''}Confira os campos destacados.`, 'erro');
            mensagensPendentes.clear();
            avisoAgendado = false;
        });
    }

    function visual(campo) {
        if (campo.classList.contains('sim-seletor-nativo')) {
            return campo.closest('.campo')?.querySelector('.sim-seletor-abrir') || campo;
        }
        return campo.type === 'radio' ? campo.closest('.markup-opcoes') || campo : campo;
    }

    function limpar(campo) {
        if (!campo) return;
        clearTimeout(temporizadores.get(campo));
        visual(campo).classList.remove('campo-erro-temporario');
        visual(campo).removeAttribute('aria-invalid');
        campo.removeAttribute('aria-invalid');
        const caixa = campo.closest('.campo')?.querySelector('.erro-campo');
        if (caixa) caixa.hidden = true;
    }

    function destacar(campo) {
        if (!campo) return;
        clearTimeout(temporizadores.get(campo));
        visual(campo).classList.add('campo-erro-temporario');
        visual(campo).setAttribute('aria-invalid', 'true');
        campo.setAttribute('aria-invalid', 'true');
        temporizadores.set(campo, setTimeout(() => limpar(campo), 3000));
    }

    function mensagem(campo) {
        const v = campo.validity;
        if (v.customError) return campo.validationMessage;
        if (v.valueMissing) return campo.dataset.mensagemObrigatoria || 'Preencha os campos obrigatórios.';
        if (v.typeMismatch) return campo.type === 'email' ? 'Informe um e-mail válido.' : 'Confira o formato informado.';
        if (v.tooShort) return `Informe pelo menos ${campo.minLength} caracteres.`;
        if (v.tooLong) return `Informe no máximo ${campo.maxLength} caracteres.`;
        if (v.rangeUnderflow) return `O valor mínimo é ${String(campo.min).replace('.', ',')}.`;
        if (v.rangeOverflow) return `O valor máximo é ${String(campo.max).replace('.', ',')}.`;
        if (v.stepMismatch) return 'Confira o valor informado.';
        return campo.dataset.mensagemFormato || 'Confira os dados informados.';
    }

    function validar(campo, notificar = true) {
        if (!campo || campo.disabled || campo.type === 'hidden' || campo.closest('[hidden]')) return true;
        if (campo.validity.valid) { limpar(campo); return true; }
        destacar(campo);
        if (notificar) avisar(mensagem(campo));
        return false;
    }

    function inicializar() {
        document.querySelectorAll('form').forEach(form => { if (form.method.toLowerCase() === 'post') form.noValidate = true; });
        document.querySelectorAll('.erro-campo').forEach(caixa => {
            const texto = caixa.textContent.trim();
            if (!texto || caixa.dataset.erroApresentado === 'true') return;
            caixa.dataset.erroApresentado = 'true';
            caixa.hidden = true;
            if (caixa.hasAttribute('data-erro-arquivo')) {
                destacar(document.getElementById('arquivoModelo3mf'));
            } else {
                const grupo = caixa.closest('.campo') || caixa.parentElement;
                grupo?.querySelectorAll('input:not([type="hidden"]), select, textarea').forEach(destacar);
            }
            avisar(texto);
        });
    }

    document.addEventListener('submit', evento => {
        const form = evento.target;
        if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== 'post' || form.id === 'formCalculo') return;
        const modeloImpressora = form.querySelector('#modelo[list="modelosBambu"]');
        if (modeloImpressora && modeloImpressora.value.trim().length < 2) {
            evento.preventDefault();
            destacar(modeloImpressora);
            const nomeImpressora = form.querySelector('#nome');
            if (nomeImpressora && !nomeImpressora.validity.valid) destacar(nomeImpressora);
            avisar('Selecione ou informe o modelo da impressora.');
            modeloImpressora.focus();
            return;
        }
        const invalidos = [...form.elements].filter(campo => campo.willValidate && !validar(campo, false));
        if (!invalidos.length) return;
        evento.preventDefault();
        avisar(mensagem(invalidos[0]));
        visual(invalidos[0]).focus();
    }, true);
    document.addEventListener('invalid', evento => {
        evento.preventDefault();
        validar(evento.target);
    }, true);
    const editar = evento => {
        if (evento.target.matches?.('input,select,textarea') && evento.target.validity.valid) limpar(evento.target);
    };
    document.addEventListener('input', editar);
    document.addEventListener('change', editar);
    document.addEventListener('DOMContentLoaded', inicializar);
    document.addEventListener('turbo:load', inicializar);
    window.validacaoInline = { validar, limpar, destacar, focar: campo => visual(campo).focus() };
    if (document.readyState !== 'loading') inicializar();
})();
