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

        const caixaIncluirFoto = document.querySelector('[data-incluir-foto]');
        const avisarFotoManual = () => window.mostrarNotificacao?.(
            'Este navegador não permite anexar a foto diretamente. A imagem foi baixada; anexe-a na conversa do WhatsApp.',
            'info'
        );

        async function baixarFoto(url) {
            const resposta = await fetch(url, { credentials: 'same-origin' });
            if (!resposta.ok) throw new Error('Não foi possível carregar a foto.');
            const blob = await resposta.blob();
            const extensao = blob.type === 'image/png' ? 'png' : blob.type === 'image/webp' ? 'webp' : 'jpg';
            const arquivo = new File([blob], `foto-peca.${extensao}`, { type: blob.type || 'image/jpeg' });
            return { blob, arquivo };
        }

        document.querySelector('[data-enviar-whatsapp]')?.addEventListener('click', async () => {
            const urlFoto = painel.dataset.fotoUrl;
            if (caixaIncluirFoto?.checked && urlFoto) {
                const abaWhatsApp = window.open('about:blank', '_blank');
                try {
                    const { blob, arquivo } = await baixarFoto(urlFoto);
                    if (navigator.share && navigator.canShare?.({ files: [arquivo] })) {
                        abaWhatsApp?.close();
                        await navigator.share({ title: `Orçamento ${painel.dataset.tenantNome || 'Criare 3D'}`, text: obterMensagem(), files: [arquivo] });
                        return;
                    }

                    const link = document.createElement('a');
                    link.href = URL.createObjectURL(blob);
                    link.download = arquivo.name;
                    link.click();
                    window.setTimeout(() => URL.revokeObjectURL(link.href), 1000);
                    const destino = `https://wa.me/?text=${encodeURIComponent(obterMensagem())}`;
                    if (abaWhatsApp) abaWhatsApp.location.href = destino;
                    else window.location.href = destino;
                    avisarFotoManual();
                    return;
                } catch (erro) {
                    if (erro?.name === 'AbortError') {
                        abaWhatsApp?.close();
                        return;
                    }
                    window.mostrarNotificacao?.('Não foi possível preparar a foto. O WhatsApp será aberto somente com a mensagem.', 'erro');
                    const destino = `https://wa.me/?text=${encodeURIComponent(obterMensagem())}`;
                    if (abaWhatsApp) abaWhatsApp.location.href = destino;
                    else window.open(destino, '_blank', 'noopener,noreferrer');
                    return;
                }
            }

            window.open(`https://wa.me/?text=${encodeURIComponent(obterMensagem())}`, '_blank', 'noopener,noreferrer');
        });
    }

    document.addEventListener('DOMContentLoaded', inicializarOrcamento);
    document.addEventListener('turbo:load', inicializarOrcamento);
})();
