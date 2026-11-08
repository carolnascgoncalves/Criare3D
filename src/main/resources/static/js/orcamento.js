(() => {
    const painel = document.querySelector('.pagina-orcamento');
    if (!painel) return;

    const formatarMoeda = valor => new Intl.NumberFormat('pt-BR', {
        style: 'currency', currency: 'BRL'
    }).format(Number(valor || 0));

    const criarMensagem = () => {
        const cliente = document.querySelector('#nomeCliente')?.value.trim();
        const peca = painel.dataset.peca || 'peça';
        const quantidade = Number(painel.dataset.unidades || 1);
        const unitario = formatarMoeda(painel.dataset.precoUnitario);
        const total = formatarMoeda(painel.dataset.precoTotal);
        return `Olá${cliente ? `, ${cliente}` : ''}! Segue o orçamento para ${peca}: ${quantidade} ${quantidade === 1 ? 'unidade' : 'unidades'}, a ${unitario} cada. Total: ${total}.`;
    };

    document.querySelector('[data-copiar-orcamento]')?.addEventListener('click', async event => {
        try {
            await navigator.clipboard.writeText(criarMensagem());
            event.currentTarget.textContent = 'Mensagem copiada';
        } catch {
            window.prompt('Copie a mensagem do orçamento:', criarMensagem());
        }
    });

    document.querySelector('[data-enviar-whatsapp]')?.addEventListener('click', () => {
        window.open(`https://wa.me/?text=${encodeURIComponent(criarMensagem())}`, '_blank', 'noopener,noreferrer');
    });
})();
