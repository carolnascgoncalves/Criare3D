(() => {
    if(window.criareContatosPedido)return;
    window.criareContatosPedido=true;
    function atualizar(caixa) {
        const campo=document.querySelector('input:not([type="hidden"])[name="contato"]');
        const contato=(campo ? campo.value : caixa.dataset.contato || '').trim();
        const campoEmail=document.querySelector('input:not([type="hidden"])[name="email"]');
        const endereco=(campoEmail ? campoEmail.value : caixa.dataset.email || (contato.includes('@')?contato:'')).trim();
        const email=/^[^\s@\r\n]+@[^\s@\r\n]+\.[^\s@\r\n]+$/.test(endereco);
        let telefone=/^[+\d\s().-]+$/.test(contato)?contato.replace(/\D/g,''):'';
        if(!contato.startsWith('+') && (telefone.length===10 || telefone.length===11))telefone='55'+telefone;
        const telefoneValido=/^\d{10,15}$/.test(telefone);
        const titulo=document.querySelector('[data-pedido-titulo]')?.value || document.querySelector('input:not([type="hidden"])[name="titulo"]')?.value || caixa.dataset.titulo;
        const itens=[...caixa.querySelectorAll('[data-pedido-item-texto]')].map(el=>el.textContent).filter(Boolean).join('\n');
        const observacoes=document.querySelector('textarea[name="observacoes"]')?.value || '';
        const proposta=document.querySelector('[data-pedido-proposta]');
        const texto=proposta ? proposta.innerText+(observacoes?'\n'+observacoes:'') : `${titulo}\n${itens}\nTotal: R$ ${caixa.dataset.total}\n${observacoes}\nPrazo e entrega conforme a proposta. Confira a prévia antes de enviar.`;
        caixa.querySelectorAll('[data-pedido-destino]').forEach(link=>{
            const whatsapp=link.dataset.pedidoDestino==='whatsapp',valido=whatsapp?telefoneValido:email;
            link.setAttribute('aria-disabled',String(!valido));
            link.href=valido?(link.dataset.pedidoDestino==='gmail'?'https://mail.google.com/mail/?view=cm&fs=1&to='+encodeURIComponent(endereco)+'&su='+encodeURIComponent(titulo)+'&body='+encodeURIComponent(texto):whatsapp?'https://wa.me/'+telefone+'?text='+encodeURIComponent(texto):'mailto:'+encodeURIComponent(endereco).replace(/%40/g,'@')+'?subject='+encodeURIComponent(titulo)+'&body='+encodeURIComponent(texto)):'#';
        });
    }
    const carregar=()=>document.querySelectorAll('[data-pedido-contatos]').forEach(atualizar);
    document.addEventListener('input',event=>{if(['contato','email'].includes(event.target.name))carregar();});
    document.addEventListener('click',event=>{
        const link=event.target.closest('[data-pedido-destino]');if(!link)return;
        atualizar(link.closest('[data-pedido-contatos]'));
        if(link.getAttribute('aria-disabled')==='true') {event.preventDefault();window.mostrarNotificacao?.('Informe um telefone com DDD para WhatsApp ou um endereço de e-mail para abrir o e-mail.','erro');}
    });
    document.addEventListener('DOMContentLoaded',carregar);document.addEventListener('turbo:load',carregar);
})();
