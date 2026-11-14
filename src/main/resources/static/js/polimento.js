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
        if(!agenda||!lista||agenda.dataset.pronto)return;agenda.dataset.pronto='true';
        const cards=[...lista.querySelectorAll('[data-agenda-inicio]')].filter(c=>Number.isFinite(new Date(c.dataset.agendaInicio).getTime())&&Number.isFinite(new Date(c.dataset.agendaFim).getTime()));
        if(!cards.length){agenda.hidden=true;const controles=document.querySelector('[data-agenda-controles]');if(controles)controles.hidden=true;return;}
        const grupos=new Map();for(const card of cards){const key=card.dataset.agendaImpressora;const g=grupos.get(key)||[];g.push(card);grupos.set(key,g);}
        const minimo=new Date(Math.min(...cards.map(c=>new Date(c.dataset.agendaInicio).getTime())));minimo.setHours(0,0,0,0);
        const maximo=new Date(Math.max(...cards.map(c=>new Date(c.dataset.agendaFim).getTime())));maximo.setHours(0,0,0,0);maximo.setDate(maximo.getDate()+1);
        let inicio=new Date(minimo),tudo=false;
        const dia=v=>new Date(v).toLocaleDateString('pt-BR',{day:'2-digit',month:'2-digit'});
        const data=v=>new Date(v).toLocaleString('pt-BR',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'});
        const elemento=(tag,classe,texto)=>{const e=document.createElement(tag);if(classe)e.className=classe;if(texto!=null)e.textContent=texto;return e;};
        function render(){
            agenda.replaceChildren();const fim=tudo?new Date(maximo):new Date(inicio);if(!tudo)fim.setDate(fim.getDate()+7);
            const de=tudo?minimo:inicio,amplitude=Math.max(1,fim-de),pos=v=>Math.max(0,Math.min(100,100*(v-de)/amplitude));
            const barra=elemento('header','agenda-navegacao');const periodo=elemento('strong','',dia(de)+' – '+dia(new Date(fim.getTime()-1)));
            const anterior=elemento('button','botao botao-secundario','‹'),proximo=elemento('button','botao botao-secundario','›'),todos=elemento('button','botao botao-secundario',tudo?'Ver semana':'Ver todo o período');
            for(const b of [anterior,proximo,todos])b.type='button';anterior.setAttribute('aria-label','Semana anterior');proximo.setAttribute('aria-label','Próxima semana');
            anterior.disabled=tudo||inicio<=minimo;proximo.disabled=tudo||fim>=maximo;
            anterior.addEventListener('click',()=>{inicio.setDate(inicio.getDate()-7);if(inicio<minimo)inicio=new Date(minimo);render();});
            proximo.addEventListener('click',()=>{inicio.setDate(inicio.getDate()+7);render();});todos.addEventListener('click',()=>{tudo=!tudo;render();});
            barra.append(periodo,anterior,proximo,todos);agenda.append(barra);
            const superficie=elemento('div','agenda-superficie');const regua=elemento('div','agenda-regua');regua.append(elemento('span','agenda-regua-titulo','Planejamento'));
            const eixo=elemento('div','agenda-eixo');const quantidade=Math.ceil(amplitude/86400000),passo=Math.max(1,Math.ceil(quantidade/8));const pontos=[];
            for(let d=new Date(de),i=0;d<fim;d.setDate(d.getDate()+1),i++){if(i%passo)continue;const p=pos(d);pontos.push(p);const marca=elemento('span','agenda-dia',dia(d));marca.style.left=p+'%';if(p>88)marca.style.transform='translateX(-100%)';eixo.append(marca);}
            const hoje=new Date();const agora=pos(hoje);const temHoje=hoje>=de&&hoje<fim;
            if(temHoje){const marca=elemento('span','agenda-hoje-titulo','Hoje');marca.style.left=agora+'%';eixo.append(marca);}
            regua.append(eixo);superficie.append(regua);
            let indice=0,visiveis=0;const cores=['var(--cor-primaria)','#329e99','#be8b39','#6e89b4'];
            for(const grupo of grupos.values()){
                const cor=cores[indice++%cores.length];const section=elemento('section','agenda-linha');section.style.setProperty('--agenda-cor',cor);
                const cabecalho=elemento('h2','agenda-impressora');const icone=elemento('i','bi bi-printer');icone.setAttribute('aria-hidden','true');cabecalho.append(icone,document.createTextNode(grupo[0].dataset.agendaNome));section.append(cabecalho);
                const eventos=grupo.filter(c=>new Date(c.dataset.agendaFim)>=de&&new Date(c.dataset.agendaInicio)<fim).sort((a,b)=>a.dataset.agendaInicio.localeCompare(b.dataset.agendaInicio));
                if(!eventos.length)section.append(elemento('p','agenda-sem-trabalho','Sem trabalhos neste período.'));
                for(const card of eventos){visiveis++;const comeco=new Date(card.dataset.agendaInicio),termino=new Date(card.dataset.agendaFim),esquerda=pos(comeco),direita=pos(termino);
                    const nome=card.querySelector('h2').textContent;const destino=card.querySelector('.planejamento-acoes a');
                    const item=elemento('article','agenda-planejamento');item.title=nome+' · '+data(comeco)+' até '+data(termino);
                    const legenda=elemento('div','agenda-legenda');const titulo=elemento('a','agenda-nome',nome);titulo.href=destino.href;
                    const datas=elemento('div','agenda-datas');const a=elemento('time','','Início: '+data(comeco)),b=elemento('time','','Previsão: '+data(termino));a.dateTime=card.dataset.agendaInicio;b.dateTime=card.dataset.agendaFim;datas.append(a,b);legenda.append(titulo,datas);
                    const acoes=card.querySelector('.planejamento-acoes')?.cloneNode(true);if(acoes){acoes.classList.add('agenda-acoes');legenda.append(acoes);}
                    const trilho=elemento('div','agenda-trilho');trilho.title=item.title;
                    for(const p of pontos){const grade=elemento('span','agenda-grade-dia');grade.style.left=p+'%';trilho.append(grade);}
                    if(temHoje){const h=elemento('span','agenda-hoje');h.style.left=agora+'%';trilho.append(h);}
                    const link=elemento('a','agenda-evento');link.href=destino.href;link.title=item.title;link.setAttribute('aria-label',item.title);link.style.left=esquerda+'%';link.style.width=Math.max(.6,direita-esquerda)+'%';link.style.maxWidth=(100-esquerda)+'%';
                    if(direita-esquerda<4)link.classList.add('agenda-evento-curto');
                    const marcos=elemento('div','agenda-marcos');if(direita-esquerda<18)marcos.classList.add('agenda-marcos-proximos');
                    const dm=elemento('span','agenda-marco inicio',comeco<de?'← '+dia(comeco):dia(comeco)),fm=elemento('span','agenda-marco fim',termino>fim?dia(termino)+' →':dia(termino));
                    dm.style.left=esquerda+'%';fm.style.left=direita+'%';dm.style.setProperty('--marco-deslocamento',esquerda<12?'0%':'-100%');fm.style.setProperty('--marco-deslocamento',direita>88?'-100%':'0%');dm.title='Início: '+data(comeco);fm.title='Previsão: '+data(termino);
                    marcos.append(dm,fm);trilho.append(marcos,link);item.append(legenda,trilho);section.append(item);
                }
                superficie.append(section);
            }
            agenda.append(superficie,elemento('p','agenda-nota',visiveis+' planejamento(s) neste período · Horários previstos, com margem de segurança. Passe o mouse para conferir os detalhes.'));
        }
        render();lista.hidden=true;
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
            if(abrir.dataset.produtoVender){const s=d.querySelector('[name=produtoId]');if(s){if(![...s.options].some(o=>o.value===abrir.dataset.produtoVender)){const o=document.createElement('option');o.value=abrir.dataset.produtoVender;o.textContent=abrir.dataset.produtoNome||'Produto selecionado';s.append(o);}s.value=abrir.dataset.produtoVender;s.dispatchEvent(new Event('change',{bubbles:true}));}}
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
