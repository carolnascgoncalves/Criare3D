(() => {
    if (window.criareSimulacaoPedido) return;
    window.criareSimulacaoPedido = true;
    const estados = new WeakMap();
    let quadro;
    function preparar(root) {
        if (estados.has(root)) return estados.get(root);
        const trechos = [...root.querySelectorAll('[data-inicio]')].map(el => ({
            inicio: new Date(el.dataset.inicio).getTime(), fim: new Date(el.dataset.fim).getTime(),
            unidades: Number(el.dataset.unidades), nome: el.dataset.nome, item: el.dataset.item || el.dataset.nome, observacao: el.dataset.observacao || '', folga: Number(el.dataset.folga || 0)*60000
        })).filter(t => Number.isFinite(t.inicio) && t.fim > t.inicio);
        if (!trechos.length) return null;
        const estado = {trechos, inicio: Math.min(...trechos.map(t => t.inicio)), fim: Math.max(...trechos.map(t => t.fim))};
        estados.set(root, estado);desenhar(root,estado);return estado;
    }
    function cor(e,t) {return `hsl(${(24 + [...new Set(e.trechos.map(x=>x.item))].indexOf(t.item)*137.5)%360} 42% 48%)`;}
    function desenhar(root,e) {
        const linha=root.querySelector('[data-pedido-linha]');if(!linha)return;
        linha.replaceChildren();const total=e.fim-e.inicio;
        function bloco(parent,inicio,fim,classe,corFundo,titulo) {
            if(fim<=inicio)return;const div=document.createElement('span');div.className=classe;div.style.left=((inicio-e.inicio)/total*100)+'%';div.style.width=((fim-inicio)/total*100)+'%';if(corFundo)div.style.backgroundColor=corFundo;div.title=titulo||'';parent.appendChild(div);
        }
        const faixas=document.createElement('div');faixas.className='pedido-faixas';linha.appendChild(faixas);
        let cursor=e.inicio,limite=0;
        while(cursor<e.fim && limite++<4000) {const d=new Date(cursor),dia=d.getHours()>=6&&d.getHours()<18;const proximo=new Date(d);proximo.setHours(dia?18:(d.getHours()<6?6:30),0,0,0);const fim=Math.min(e.fim,proximo.getTime());bloco(faixas,cursor,fim,dia?'pedido-faixa-dia':'pedido-faixa-noite',null,dia?'Dia':'Noite');cursor=fim;}
        // Um único eixo de tempo. Impressões simultâneas ocupam trilhas internas na mesma barra.
        const fins=[];const ordenados=[...e.trechos].sort((a,b)=>a.inicio-b.inicio);
        const posicionados=ordenados.map(t=>{let pista=fins.findIndex(fim=>fim<=t.inicio);if(pista<0)pista=fins.length;fins[pista]=t.fim;return {t,pista};});
        const pistas=Math.max(1,fins.length);
        for(const {t,pista} of posicionados){const fim=t.fim-Math.min(t.folga,t.fim-t.inicio);for(const [inicio,termino,classe,fundo,titulo] of [[t.inicio,fim,'pedido-faixa-impressao',cor(e,t),t.nome],[fim,t.fim,'pedido-faixa-folga',null,t.nome+' · folga']]){const anterior=faixas.children.length;bloco(faixas,inicio,termino,classe,fundo,titulo);if(faixas.children.length>anterior){const el=faixas.lastElementChild;el.style.top=(8+(pista+.5)*44/pistas-Math.min(24,40/pistas)/2)+'px';el.style.height=Math.min(24,40/pistas)+'px';}}}
        const marcador=document.createElement('span');marcador.className='pedido-faixa-cursor';faixas.appendChild(marcador);e.cursor=marcador;
        const resumo=root.querySelector('[data-sim-resumo]');
        if(resumo){resumo.replaceChildren();const formatoResumo=new Intl.DateTimeFormat('pt-BR',{weekday:'short',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'});const tempo=ms=>{const m=Math.round(ms/60000);return m>=60?`${Math.floor(m/60)} h${m%60?' '+m%60+' min':''}`:`${m} min`;};
            for(const item of [...new Set(ordenados.map(t=>t.item))]){const ts=ordenados.filter(t=>t.item===item),inicio=Math.min(...ts.map(t=>t.inicio)),fim=Math.max(...ts.map(t=>t.fim)),folga=ts.reduce((v,t)=>v+t.folga,0),impressao=ts.reduce((v,t)=>v+t.fim-t.inicio-t.folga,0),espera=Math.max(0,fim-inicio-folga-impressao);const artigo=document.createElement('article');artigo.style.borderLeftColor=cor(e,ts[0]);const nome=document.createElement('strong');nome.textContent=ts[0].nome;artigo.appendChild(nome);for(const texto of [`${formatoResumo.format(new Date(inicio))} → ${formatoResumo.format(new Date(fim))} · Duração total: ${tempo(fim-inicio)} · Impressão: ${tempo(impressao)}`]){const linha=document.createElement('span');linha.textContent=texto;artigo.appendChild(linha);}if(ts[0].observacao){const motivo=document.createElement("small");motivo.textContent=ts[0].observacao;artigo.appendChild(motivo);}resumo.appendChild(artigo);}
        }
        const formato=new Intl.DateTimeFormat('pt-BR',{weekday:'long',day:'2-digit',month:'long',hour:'2-digit',minute:'2-digit'});
        for(const [campo,data] of [['inicio',e.inicio],['fim',e.fim]]) {const no=root.querySelector('[data-sim-'+campo+']');if(no)no.textContent=formato.format(new Date(data));}
    }
    function mostrar(root, fracao) {
        const e = preparar(root);if (!e) return;
        const horario = e.inicio + (e.fim - e.inicio) * fracao;
        if(e.cursor)e.cursor.style.left=`calc(${fracao*100}% - ${fracao>=1?2:0}px)`;
        const terminadas = e.trechos.filter(t => t.fim <= horario).reduce((s,t) => s + t.unidades, 0);
        const imprimindo = e.trechos.filter(t => t.inicio <= horario && t.fim > horario);
        const progresso=root.querySelector('[data-sim-progresso]');if(progresso)progresso.value = fracao * 100;
        root.querySelector('[data-sim-prontas]').textContent = String(terminadas);
        root.querySelector('[data-sim-estado]').textContent = fracao >= 1 ? 'Previsão concluída' : imprimindo.length ? 'Imprimindo: ' + imprimindo.map(t=>t.nome).join(', ') : 'Aguardando próximo início';
        const ultima=[...e.trechos].filter(t=>t.fim<=horario).sort((a,b)=>b.fim-a.fim)[0];
        const proxima=[...e.trechos].filter(t=>t.inicio>horario).sort((a,b)=>a.inicio-b.inicio)[0];
        const atual=imprimindo[0] || ultima || null;
        const vaso=root.querySelector('.sim-mesa');
        const rotulo=root.querySelector('[data-sim-item-label]');if(rotulo)rotulo.textContent=imprimindo.length?'Peça em impressão':'Última peça simulada';
        const nome=root.querySelector('[data-sim-item]');if(nome)nome.textContent=atual?atual.nome+(imprimindo.length>1?' · outras peças em paralelo':''):'Pausa entre impressões';
        const espera=root.querySelector('[data-sim-espera]');if(espera){espera.hidden=imprimindo.length>0||!proxima;espera.textContent=proxima?'Próxima impressão: '+proxima.nome+' · '+new Intl.DateTimeFormat('pt-BR',{weekday:'short',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}).format(new Date(proxima.inicio))+'.':'';}
        const camada=atual?(atual.fim<=horario?1:Math.min(1,Math.max(0,(horario-atual.inicio)/Math.max(1,atual.fim-atual.folga-atual.inicio)))):0;
        if(vaso && atual)vaso.style.setProperty('--cor-peca',cor(e,atual));
        const y=135-camada*91;root.querySelector('[data-pedido-corte]')?.setAttribute('y',String(y));
        const portal=root.querySelector('[data-pedido-portal]');portal?.setAttribute('y1',String(y-12));portal?.setAttribute('y2',String(y-12));
        root.querySelector('[data-pedido-bico]')?.setAttribute('transform',`translate(${100+Math.sin(fracao*300)*25} ${y-12})`);
        const continuar=root.querySelector('[data-sim-continuar]');if(continuar)continuar.hidden=fracao<1;
        const data = new Date(horario);
        root.querySelector('[data-sim-relogio]').textContent = new Intl.DateTimeFormat('pt-BR',{weekday:'short',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}).format(data);
        root.querySelector('[data-sim-periodo]').textContent = data.getHours() >= 6 && data.getHours() < 18 ? 'Dia (6h–18h)' : 'Noite (18h–6h)';
    }
    function iniciar(root) {
        cancelAnimationFrame(quadro);
        if (!preparar(root)) return;
        if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {mostrar(root,1);return;}
        const continuar=root.querySelector('[data-sim-continuar]');if(continuar)continuar.hidden=true;
        const inicio = performance.now();
        function animar(agora) {
            if (!root.isConnected) return;
            const fracao = Math.min(1, (agora - inicio) / 12000);mostrar(root,fracao);
            if(fracao < 1) quadro = requestAnimationFrame(animar);
        }
        quadro = requestAnimationFrame(animar);
    }
    function carregar() {
        document.querySelectorAll('[data-pedido-simulacao]').forEach(root => {
            if(root.dataset.auto === 'true') {root.dataset.auto='false';root.hidden=false;preparar(root);mostrar(root,1);}
        });
    }
    document.addEventListener('click', event => {
        const pular = event.target.closest('[data-sim-pular]'), repetir = event.target.closest('[data-sim-repetir]');
        if(pular) {cancelAnimationFrame(quadro);mostrar(pular.closest('[data-pedido-simulacao]'),1);}
        if(repetir){const root=repetir.closest('[data-pedido-simulacao]');const animacao=root.querySelector('.pedido-animacao');if(animacao)animacao.open=true;iniciar(root);}
    });
    document.addEventListener('turbo:before-cache', () => {cancelAnimationFrame(quadro);document.querySelectorAll('[data-pedido-simulacao]').forEach(root=>{root.hidden=true;root.dataset.auto='true';estados.delete(root);});});
    document.addEventListener('input',event=>{
        if(!event.target.closest('.pedido-dados'))return;
        if(!['inicioPretendido','turnoInicio','turnoFim','dias','intervalo','folga'].includes(event.target.name))return;
        cancelAnimationFrame(quadro);document.querySelectorAll('[data-pedido-simulacao]').forEach(root=>root.hidden=true);
    });
    document.addEventListener('turbo:frame-load',carregar);document.addEventListener('pedido:preview',carregar);document.addEventListener('DOMContentLoaded',carregar);document.addEventListener('turbo:load',carregar);
})();
