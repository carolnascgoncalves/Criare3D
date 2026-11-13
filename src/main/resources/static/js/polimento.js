(() => {
    if (window.polimentoUiAtivo) return;
    window.polimentoUiAtivo = true;
    const normalizar = s => String(s).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
    function iniciar() {
        document.querySelectorAll('.arquivo-soltar').forEach(area => {
            if (area.dataset.soltarPronto) return;
            area.dataset.soltarPronto='true';
            const input=area.querySelector('input[type=file]');
            if (!input) return;
            area.addEventListener('dragover',e=>{e.preventDefault();area.classList.add('arrastando');});
            area.addEventListener('dragleave',()=>area.classList.remove('arrastando'));
            area.addEventListener('drop',e=>{
                e.preventDefault();area.classList.remove('arrastando');
                const file=e.dataTransfer.files[0];if(!file) return;
                const transfer=new DataTransfer();transfer.items.add(file);input.files=transfer.files;
                input.dispatchEvent(new Event('change',{bubbles:true}));
                area.querySelector('[data-analisar-3mf]')?.click();
            });
        });
        // Submit the existing GET search after a pause; server pagination/search stay authoritative.
        document.querySelectorAll('.lista-catalogo form.busca-filamentos input[name=busca]').forEach(input=>{
            if(input.dataset.buscaViva)return;input.dataset.buscaViva='true';
            let timer;input.addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(()=>{if(input.isConnected)input.form.requestSubmit();},700);});
            document.addEventListener('turbo:before-cache',()=>clearTimeout(timer),{once:true});
        });
        const filtro=document.querySelector('[data-filtrar-acabando]');
        if(filtro && !filtro.dataset.pronto){
            filtro.dataset.pronto='true';const linhas=[...document.querySelectorAll('[data-estoque-baixo]')];
            filtro.querySelector('[data-total-acabando]').textContent=`(${linhas.filter(l=>l.dataset.estoqueBaixo==='true').length})`;
            filtro.addEventListener('click',()=>{const ativo=filtro.getAttribute('aria-pressed')!=='true';filtro.setAttribute('aria-pressed',String(ativo));linhas.forEach(l=>l.hidden=ativo&&l.dataset.estoqueBaixo!=='true');});
        }
        const markup=document.getElementById('markupPercentual'),exemplo=document.querySelector('[data-exemplo-markup]');
        if(markup&&exemplo&&!markup.dataset.exemploPronto){markup.dataset.exemploPronto='true';const atualizar=()=>{exemplo.textContent=`R$ 10,00 de custo vira ${(10*(1+Number(markup.value||0)/100)).toLocaleString('pt-BR',{style:'currency',currency:'BRL'})} antes das taxas.`;};markup.addEventListener('input',atualizar);atualizar();}
        document.querySelectorAll('dialog[data-abrir-inicial=true]').forEach(d=>{if(!d.open)d.showModal();});
        if(location.hash==='#valorMaoObraHora') requestAnimationFrame(()=>{
            const ativar=document.getElementById('maoObraAtivada');
            if(ativar&&!ativar.checked) ativar.click();
            const valor=document.getElementById('valorMaoObraHora');
            valor?.scrollIntoView({block:'center'});valor?.focus({preventScroll:true});
        });
        criarAgenda();
    }
    function criarAgenda(){
        const agenda=document.querySelector('[data-agenda]'),lista=document.querySelector('[data-agenda-lista]');
        if(!agenda||agenda.dataset.pronto)return;agenda.dataset.pronto='true';
        const cards=[...lista.querySelectorAll('[data-agenda-inicio]')];
        if(!cards.length){agenda.hidden=true;document.querySelector('[data-agenda-controles]').hidden=true;return;}
        const grupos=new Map();for(const card of cards){const key=card.dataset.agendaImpressora;const g=grupos.get(key)||[];g.push(card);grupos.set(key,g);}
        const datas=cards.flatMap(c=>[new Date(c.dataset.agendaInicio).getTime(),new Date(c.dataset.agendaFim).getTime()]).filter(Number.isFinite);
        if(!datas.length){agenda.hidden=true;return;}
        const min=Math.min(...datas),max=Math.max(...datas),duracao=Math.max(3600000,max-min);
        const data=v=>new Date(v).toLocaleString('pt-BR',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'});
        const periodo=document.createElement('p');periodo.className='texto-apoio';periodo.textContent=`${data(min)} → ${data(max)}`;agenda.append(periodo);
        for(const grupo of grupos.values()){
            const linha=document.createElement('section');linha.className='agenda-linha';
            const h=document.createElement('h2');h.textContent=grupo[0].dataset.agendaNome;linha.append(h);
            for(const card of grupo.sort((a,b)=>a.dataset.agendaInicio.localeCompare(b.dataset.agendaInicio))){
                const inicio=new Date(card.dataset.agendaInicio).getTime(),fim=new Date(card.dataset.agendaFim).getTime();
                if(!Number.isFinite(inicio)||!Number.isFinite(fim))continue;
                const faixa=document.createElement('div');faixa.className='agenda-trilho';
                const link=document.createElement('a');link.className='agenda-evento';link.href=card.querySelector('.planejamento-acoes a').href;
                link.style.marginLeft=`${100*(inicio-min)/duracao}%`;link.style.width=`${Math.max(3,100*(fim-inicio)/duracao)}%`;
                link.style.maxWidth=`${100-100*(inicio-min)/duracao}%`;
                const nome=card.querySelector('h2').textContent;link.title=`${nome} · ${data(inicio)} até ${data(fim)}`;link.setAttribute('aria-label',link.title);link.textContent=nome;
                faixa.append(link);linha.append(faixa);
                const legenda=document.createElement('small');legenda.textContent=link.title;linha.append(legenda);
            }
            agenda.append(linha);
        }
        lista.hidden=true;
    }
    document.addEventListener('click',e=>{
        const abrir=e.target.closest('[data-dialogo-abrir]');
        if(abrir){const d=document.getElementById(abrir.dataset.dialogoAbrir);if(!d)return;
            if(abrir.dataset.produtoVender){const s=d.querySelector('[name=produtoId]');if(s){s.value=abrir.dataset.produtoVender;s.dispatchEvent(new Event('change',{bubbles:true}));}}
            d.showModal();
        }
        e.target.closest('[data-dialogo-fechar]')?.closest('dialog')?.close();
        const atalho=e.target.closest('[data-markup-atalho]');if(atalho){const i=document.getElementById('rapidoMarkup');if(i&&!i.readOnly){i.value=atalho.dataset.markupAtalho;i.dispatchEvent(new Event('input',{bubbles:true}));}}
        const modo=e.target.closest('[data-agenda-modo]');if(modo){const agenda=modo.dataset.agendaModo==='agenda';document.querySelector('[data-agenda]').hidden=!agenda;document.querySelector('[data-agenda-lista]').hidden=agenda;document.querySelectorAll('[data-agenda-modo]').forEach(b=>b.setAttribute('aria-pressed',String(b===modo)));}
        document.querySelectorAll('.acoes-menu[open]').forEach(menu=>{if(!menu.contains(e.target))menu.open=false;});
    });
    document.addEventListener('keydown',e=>{if(e.key==='Escape')document.querySelectorAll('.acoes-menu[open]').forEach(m=>{m.open=false;m.querySelector('summary').focus();});});
    document.addEventListener('DOMContentLoaded',iniciar);
    document.addEventListener('turbo:load',iniciar);
    document.addEventListener('turbo:before-cache',()=>{
        document.querySelectorAll('.formulario-dialogo[open]').forEach(d=>d.close());
        document.querySelectorAll('[data-soltar-pronto]').forEach(e=>delete e.dataset.soltarPronto);
        document.querySelectorAll('[data-busca-viva]').forEach(e=>delete e.dataset.buscaViva);
        document.querySelectorAll('[data-exemplo-pronto]').forEach(e=>delete e.dataset.exemploPronto);
        document.querySelectorAll('[data-filtrar-acabando]').forEach(e=>delete e.dataset.pronto);
        document.querySelectorAll('[data-agenda]').forEach(e=>{delete e.dataset.pronto;e.replaceChildren();});
        document.querySelectorAll('[data-agenda-lista]').forEach(e=>e.hidden=false);
    });
})();
