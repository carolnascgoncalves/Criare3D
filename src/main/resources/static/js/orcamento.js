(() => {
    function inicializarOrcamento() {
        const painel = document.querySelector('.pagina-orcamento');
        if (!painel || painel.dataset.orcamentoInicializado === 'true') return;
        painel.dataset.orcamentoInicializado = 'true';

        const campoNome = document.querySelector('#nomeCliente');
        const campoPrazo = document.querySelector('#prazoOrcamento');
        const campoPagamento = document.querySelector('#pagamentoOrcamento');
        const campoValidade = document.querySelector('#validadeOrcamento');
        const campoMensagem = document.querySelector('#mensagemOrcamento');
        const editor = document.querySelector('[data-editor-personalizacao]');
        const botaoEditor = document.querySelector('[data-alternar-personalizacao]');

        const formatarMoeda = valor => new Intl.NumberFormat('pt-BR', {
            style: 'currency', currency: 'BRL'
        }).format(Number(valor || 0));

        const gerarMensagemPadrao = () => {
            const nome = campoNome?.value.trim();
            const peca = painel.dataset.peca || 'Peça';
            const quantidade = Number(painel.dataset.unidades || 1);
            const textoQuantidade = `${quantidade} ${quantidade === 1 ? 'unidade' : 'unidades'}`;
            const unitario = formatarMoeda(painel.dataset.precoUnitario);
            const total = formatarMoeda(painel.dataset.precoTotal);
            const prazo = campoPrazo?.value.trim() || 'A combinar';
            const pagamento = campoPagamento?.value.trim() || 'A combinar';
            const validade = campoValidade?.value.trim() || 'A combinar';
            const saudacao = nome ? `Olá, ${nome}! Tudo bem?` : 'Olá! Tudo bem?';

            return `${saudacao}\n\n` +
                'Segue o orçamento solicitado:\n\n' +
                `*${peca}*\n\n` +
                `- Quantidade: ${textoQuantidade}\n` +
                `- Valor unitário: ${unitario}\n` +
                `- *Total: ${total}*\n\n` +
                'Condições:\n\n' +
                `- Prazo de produção/entrega: ${prazo}\n` +
                `- Forma de pagamento: ${pagamento}\n` +
                `- Validade do orçamento: ${validade}`;
        };

        const atualizarMensagemPadrao = () => {
            if (campoMensagem && campoMensagem.dataset.editadaManualmente !== 'true') {
                campoMensagem.value = gerarMensagemPadrao();
            }
        };

        atualizarMensagemPadrao();
        [campoNome, campoPrazo, campoPagamento, campoValidade].forEach(campo => {
            campo?.addEventListener('input', atualizarMensagemPadrao);
        });
        campoMensagem?.addEventListener('input', () => {
            campoMensagem.dataset.editadaManualmente = 'true';
        });

        botaoEditor?.addEventListener('click', () => {
            const mostrar = editor?.classList.contains('campo-oculto');
            editor?.classList.toggle('campo-oculto', !mostrar);
            botaoEditor.setAttribute('aria-expanded', String(Boolean(mostrar)));
            if (mostrar) campoMensagem?.focus();
        });

        document.querySelector('[data-restaurar-mensagem]')?.addEventListener('click', () => {
            if (!campoMensagem) return;
            delete campoMensagem.dataset.editadaManualmente;
            campoMensagem.value = gerarMensagemPadrao();
        });

        const obterMensagem = () => campoMensagem?.value.trim() || gerarMensagemPadrao();

        document.querySelector('[data-copiar-orcamento]')?.addEventListener('click', async event => {
            try {
                await navigator.clipboard.writeText(obterMensagem());
                event.currentTarget.innerHTML = '<i class="bi bi-check-lg"></i> Mensagem copiada';
            } catch {
                window.prompt('Copie a mensagem do orçamento:', obterMensagem());
            }
        });

        document.querySelector('[data-enviar-whatsapp]')?.addEventListener('click', () => {
            window.open(`https://wa.me/?text=${encodeURIComponent(obterMensagem())}`, '_blank', 'noopener,noreferrer');
        });
    }

    document.addEventListener('DOMContentLoaded', inicializarOrcamento);
    document.addEventListener('turbo:load', inicializarOrcamento);
})();
