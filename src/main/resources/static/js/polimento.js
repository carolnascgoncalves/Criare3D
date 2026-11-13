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
            filtro.addEventListener('click',()=>{const ativo=filtro.getAttribute('aria-pressed')!=='true';filtro.setAttribute('aria-pressed',String(ativo));linhas.forEach(l=>l.hidden=ativo&&l.dataset.estoqueBaixo!=='true');const legenda=document.querySelector('[data-estado-filtro-acabando]');if(legenda)legenda.textContent=ativo?'Filtro ativo: somente filamentos acabando nesta página.':'Mostrando todos os filamentos desta página.';});
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
        criarCalendarioOcupacao();
    }
    function criarCalendarioOcupacao(){
        document.querySelectorAll('[data-calendario-ocupacao]').forEach(cal=>{
            if(cal.dataset.pronto)return;cal.dataset.pronto='true';
            const input=document.querySelector('[name=inicioPretendido]'),area=cal.querySelector('[data-calendario-conteudo]');
            if(!input||!area)return;
            let mes=new Date(input.value||Date.now());mes.setDate(1);
            const ocupacoes=[...cal.querySelectorAll('[data-ocupacao]')].map(e=>({inicio:new Date(e.dataset.inicio),fim:new Date(e.dataset.fim),nome:e.dataset.nome,id:e.dataset.impressoraId||e.dataset.nome,impressora:e.dataset.impressora||e.dataset.nome}));
            const legenda=cal.querySelector('[data-calendario-legenda]'),hora=cal.querySelector('[data-calendario-hora]'),selecao=cal.querySelector('[data-calendario-selecao]');
            const ids=[...new Set(ocupacoes.map(o=>o.id))];
            const cor=id=>{const indice=ids.indexOf(id),matiz=(indice*67)%360;return `hsl(from var(--cor-primaria, #8274bb) calc(h + ${matiz}) 45% 88%)`;};
            if(legenda){legenda.replaceChildren();ids.forEach(id=>{const linha=document.createElement('div'),amostra=document.createElement('span'),nome=document.createElement('span');linha.className='calendario-impressora';amostra.className='calendario-amostra';amostra.style.background=cor(id);nome.textContent=ocupacoes.find(o=>o.id===id).impressora;linha.append(amostra,nome);legenda.append(linha);});if(!ids.length)legenda.textContent='Nenhuma produção agendada nestas impressoras.';}
            if(hora){hora.value=input.value.split('T')[1]?.slice(0,5)||'08:00';hora.addEventListener('change',event=>{event.stopPropagation();if(input.value&&hora.value){input.value=input.value.slice(0,10)+'T'+hora.value;input.dispatchEvent(new Event('change',{bubbles:true}));render();}});}
            const chave=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
            function render(){
                if(selecao)selecao.textContent=input.value?'Dia escolhido: '+new Date(input.value).toLocaleDateString('pt-BR'):'Selecione um dia no calendário.';
                area.replaceChildren();const nav=document.createElement('div');nav.className='calendario-navegacao';
                const titulo=document.createElement('strong');titulo.textContent=mes.toLocaleDateString('pt-BR',{month:'long',year:'numeric'});
                const botao=(texto,acao)=>{const b=document.createElement('button');b.type='button';b.textContent=texto;b.addEventListener('click',acao);return b;};
                nav.append(botao('‹',()=>{mes.setMonth(mes.getMonth()-1);render();}),titulo,botao('›',()=>{mes.setMonth(mes.getMonth()+1);render();}));area.append(nav);
                const grade=document.createElement('div');grade.className='calendario-dias';
                ['D','S','T','Q','Q','S','S'].forEach(d=>{const e=document.createElement('span');e.textContent=d;grade.append(e);});
                for(let n=0;n<mes.getDay();n++)grade.append(document.createElement('span'));
                const ultimo=new Date(mes.getFullYear(),mes.getMonth()+1,0).getDate();
                for(let n=1;n<=ultimo;n++){
                    const dia=new Date(mes.getFullYear(),mes.getMonth(),n),fim=new Date(mes.getFullYear(),mes.getMonth(),n+1);
                    const conflitos=ocupacoes.filter(o=>o.inicio<fim&&o.fim>dia);
                    const b=botao(n,()=>{input.value=chave(dia)+'T'+(hora?.value||input.value.split('T')[1]||'08:00');input.dispatchEvent(new Event('change',{bubbles:true}));render();});
                    const cores=[...new Set(conflitos.map(o=>o.id))].map(cor);
                    if(cores.length===1)b.style.background=cores[0];
                    if(cores.length>1)b.style.background=`linear-gradient(135deg, ${cores.flatMap((c,i)=>[`${c} ${100*i/cores.length}%`,`${c} ${100*(i+1)/cores.length}%`]).join(', ')})`;
                    b.classList.toggle('ocupado',conflitos.length>0);b.setAttribute('aria-pressed',String(input.value.slice(0,10)===chave(dia)));
                    b.title=dia.toLocaleDateString('pt-BR')+(conflitos.length?' · Ocupado: '+conflitos.map(o=>o.nome+' ('+o.inicio.toLocaleString('pt-BR')+' até '+o.fim.toLocaleString('pt-BR')+')').join('; '):' · Sem produção agendada');
                    b.setAttribute('aria-label',b.title);grade.append(b);
                }area.append(grade);
            }
            input.addEventListener('input',()=>{if(input.value){mes=new Date(input.value);mes.setDate(1);}render();});render();
        });
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
                link.style.marginLeft=`${100*(inicio-min)/duracao}%`;link.style.width=`${Math.max(.3,100*(fim-inicio)/duracao)}%`;
                link.style.maxWidth=`${100-100*(inicio-min)/duracao}%`;
                const nome=card.querySelector('h2').textContent;link.title=`${nome} · ${data(inicio)} até ${data(fim)}`;link.setAttribute('aria-label',link.title);link.textContent=nome;
                const item=document.createElement('article');item.className='agenda-planejamento';item.title=link.title;
                faixa.title=link.title;faixa.append(link);
                const marcos=document.createElement('div');marcos.className='agenda-marcos';
                const curto=v=>new Date(v).toLocaleDateString('pt-BR',{day:'2-digit',month:'2-digit'});
                const esquerda=100*(inicio-min)/duracao,direita=100*(fim-min)/duracao;
                const deMarco=document.createElement('span'),ateMarco=document.createElement('span');
                deMarco.textContent=curto(inicio);ateMarco.textContent=curto(fim);
                deMarco.className='agenda-marco inicio';ateMarco.className='agenda-marco fim';
                deMarco.style.left=esquerda+'%';ateMarco.style.left=direita+'%';
                deMarco.style.setProperty('--marco-deslocamento',esquerda<14?'0%':'-100%');
                ateMarco.style.setProperty('--marco-deslocamento',direita>86?'-100%':'0%');
                deMarco.title='Início: '+data(inicio);ateMarco.title='Previsão: '+data(fim);
                if(direita-esquerda<14){marcos.classList.add('agenda-marcos-proximos');}
                marcos.append(deMarco,ateMarco);item.append(marcos,faixa);
                const legenda=document.createElement('div');legenda.className='agenda-legenda';
                const titulo=document.createElement('strong');titulo.textContent=nome;
                const datas=document.createElement('span');datas.className='agenda-datas';
                const de=document.createElement('time'),ate=document.createElement('time');
                de.dateTime=card.dataset.agendaInicio;de.textContent='Início: '+data(inicio);
                ate.dateTime=card.dataset.agendaFim;ate.textContent='Previsão: '+data(fim);
                datas.append(de,ate);legenda.append(titulo,datas);item.append(legenda);linha.append(item);
            }
            agenda.append(linha);
        }
        lista.hidden=true;
    }
    document.addEventListener('click',e=>{
        const indice=e.target.closest('.configuracao-indice a[href^="#"]');
        if(indice){
            const destino=document.getElementById(indice.getAttribute('href').slice(1));
            if(destino){
                e.preventDefault();e.stopPropagation();
                document.querySelectorAll('.configuracao-indice a').forEach(a=>a.removeAttribute('aria-current'));
                indice.setAttribute('aria-current','location');
                destino.setAttribute('tabindex','-1');destino.focus({preventScroll:true});
                destino.scrollIntoView({behavior:'instant',block:'start'});
                destino.classList.add('configuracao-destino');
                setTimeout(()=>destino.classList.remove('configuracao-destino'),1800);
            }
        }
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
        document.querySelectorAll('[data-calendario-ocupacao]').forEach(e=>{delete e.dataset.pronto;e.querySelector('[data-calendario-conteudo]').replaceChildren();});
        document.querySelectorAll('[data-agenda]').forEach(e=>{delete e.dataset.pronto;e.replaceChildren();});
        document.querySelectorAll('[data-agenda-lista]').forEach(e=>e.hidden=false);
    });
})();
