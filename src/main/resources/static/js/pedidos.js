(() => {
    if (window.criarePedidosInicializado) return;
    window.criarePedidosInicializado = true;
    document.addEventListener('error', event => {
        if (event.target.matches?.('[data-pedido-foto]')) event.target.remove();
    }, true);
    document.addEventListener('input', event => {
        if (!event.target.matches('[data-pedido-busca]')) return;
        const form = event.target.closest('form');
        const normalizar = valor => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
        const termo = normalizar(event.target.value);
        let visiveis = 0;
        form.querySelectorAll('[data-pedido-referencia]').forEach(linha => {
            linha.hidden = !normalizar(linha.dataset.pedidoReferencia).includes(termo);
            if (!linha.hidden) visiveis++;
        });
        const vazio = form.querySelector('[data-pedido-sem-resultados]');
        if (vazio) vazio.hidden = visiveis > 0;
    });
    function ajustarEstoque(form) {
        const selecionado = form.querySelector('input[name="referencia"]:checked');
        const estoque = form.querySelector('[data-pedido-estoque]');
        const quantidade = form.querySelector('[name="quantidade"]');
        if (!estoque) return;
        estoque.max = String(Math.max(0, Math.min(Number(selecionado?.dataset.disponivel ?? form.dataset?.disponivel ?? 0), Number(quantidade?.value || 0))));
        if (Number(estoque.value) > Number(estoque.max)) estoque.value = estoque.max;
        estoque.readOnly = Number(estoque.max) === 0;
        const titulo = form.querySelector('[data-pedido-selecionada]');
        if (titulo && selecionado) titulo.textContent = selecionado.dataset.nome || 'Peça selecionada';
        const resumo = form.querySelector('[data-pedido-origem]');
        if (resumo) {
            const total = Math.max(0, Number(quantidade?.value || 0));
            const prontas = Math.max(0, Math.min(Number(estoque.value || 0), Number(estoque.max)));
            resumo.textContent = selecionado
                ? `Pedido: ${total} un. — ${prontas} do estoque e ${Math.max(0, total - prontas)} para produzir. Neste lote há ${Number(selecionado.dataset.disponivel || 0)} un. livres.`
                : 'Escolha uma peça para conferir o estoque e a quantidade a produzir.';
        }
    }
    document.addEventListener('change', event => {
        const form = event.target.closest('.pedido-adicionar, .pedido-editar-item');
        if (form) {
            ajustarEstoque(form);

        }
    });
    document.addEventListener('input', event => {
        const form = event.target.closest('.pedido-adicionar, .pedido-editar-item');
        if (form && event.target.matches('[name="quantidade"], [data-pedido-estoque]')) ajustarEstoque(form);
    });
    document.addEventListener('turbo:load', () => document.querySelectorAll('.pedido-adicionar, .pedido-editar-item').forEach(ajustarEstoque));
    document.addEventListener('DOMContentLoaded', () => document.querySelectorAll('.pedido-adicionar, .pedido-editar-item').forEach(ajustarEstoque));
    function multi(row) {
        const marcado=row.querySelector('[data-pedido-marcar]');const campos=row.querySelector('[data-pedido-multi-campos]');const quantidade=row.querySelector('[data-multi-quantidade]');const estoque=row.querySelector('[data-multi-estoque]');
        campos.hidden=!marcado.checked;quantidade.disabled=estoque.disabled=!marcado.checked;
        estoque.max=String(Math.max(0,Math.min(Number(marcado.dataset.disponivel),Number(quantidade.value||0))));
        estoque.readOnly=Number(estoque.max)===0;if(Number(estoque.value)>Number(estoque.max))estoque.value=estoque.max;
    }
    document.addEventListener('change',e=>{const row=e.target.closest('[data-pedido-multi]');if(row)multi(row);});
    document.addEventListener('input',e=>{const row=e.target.closest('[data-pedido-multi]');if(row)multi(row);});
    document.addEventListener('turbo:load',()=>document.querySelectorAll('[data-pedido-multi]').forEach(multi));
    document.addEventListener('DOMContentLoaded',()=>document.querySelectorAll('[data-pedido-multi]').forEach(multi));
    document.addEventListener('click', async event => {
        if (event.target.closest('[data-pedido-imprimir]')) window.print();
        const copiar = event.target.closest('[data-pedido-copiar]');
        if (!copiar) return;
        try {
            await navigator.clipboard.writeText(document.querySelector('[data-pedido-proposta]').innerText);
            copiar.textContent = 'Mensagem copiada';
        } catch { copiar.textContent = 'Selecione e copie o texto da proposta'; }
    });
})();

// Serializa os salvamentos e mantém os inputs no DOM: foco e rolagem não mudam.
(() => {
 if(window.criarePedidoAutosave)return;window.criarePedidoAutosave=true;
 let fila=Promise.resolve(),pendentes=0;const sujos=new Set(),falhas=new Map(),automaticos=new WeakSet(),liberados=new WeakSet(),temporizadores=new WeakMap(),edicoes=new WeakMap();
 const moeda=v=>new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(Number(v));
 function status(texto,erro=false){document.querySelectorAll('[data-pedido-save-status],[data-pedido-barra-status]').forEach(no=>{no.textContent=texto;no.classList.toggle('pedido-save-erro',erro);});document.querySelectorAll('[data-pedido-retry]').forEach(no=>no.hidden=!erro);}
 function formDo(campo){return campo.closest?.('[data-autosave-url]');}
 function atualizar(dados){
  if(dados.rentabilidade)document.querySelectorAll('[data-pedido-rentabilidade]').forEach(no=>{const r=dados.rentabilidade;no.dataset.estado=r.estado;no.querySelector('[data-rentabilidade-titulo]').textContent=r.titulo;no.querySelector('[data-rentabilidade-lucro]').textContent=moeda(r.lucro);no.querySelector('[data-rentabilidade-margem]').textContent=new Intl.NumberFormat('pt-BR',{maximumFractionDigits:1}).format(Number(r.margem))+'%';});
  if(dados.materiais){const aviso=document.querySelector('[data-pedido-aviso-materiais]');if(aviso){aviso.textContent=dados.materiais.join(' · ');aviso.classList.toggle('pedido-aviso-material',dados.materiais.length>0);}}
  document.querySelectorAll('[data-valor-total="true"]').forEach(no=>no.textContent=moeda(dados.total));
  document.querySelectorAll('[name="revisao"]').forEach(no=>no.value=dados.revisao);
  for(const i of dados.itens||[]){const artigo=document.querySelector('[data-item-id="'+i.id+'"]');if(!artigo)continue;const total=artigo.querySelector('[data-item-total]');if(total)total.textContent='Subtotal: '+moeda(i.total);const origem=artigo.querySelector('[data-item-origem]');if(origem)origem.textContent=i.estoque+' do estoque · '+i.produzir+' a produzir';}
 }
 function formularioPronto(form){return [...form.elements].every(c=>!c.willValidate||c.validity.valid);}
 function salvar(form){
  if(!form.isConnected||!formularioPronto(form)){sujos.add(form);return;}
  sujos.delete(form);const edicao=edicoes.get(form);const corpo=new FormData(form);pendentes++;status('Salvando…');
  fila=fila.then(async()=>{
   try{
    const resposta=await fetch(form.dataset.autosaveUrl,{method:'POST',body:corpo,headers:{Accept:'application/json'},credentials:'same-origin'});
    if(!resposta.headers.get('content-type')?.includes('application/json'))throw new Error('Não foi possível salvar. Confira sua conexão e se a sessão continua aberta.');
    const dados=await resposta.json();if(!resposta.ok)throw new Error(dados.erro||'Não foi possível salvar. Confira os campos.');
    atualizar(dados);falhas.delete(form);
    if(form.dataset.etapa==='2'){
     const respostaPrazo=await fetch(location.pathname+'?etapa=2',{headers:{Accept:'text/html'},credentials:'same-origin'});
     if(!respostaPrazo.ok)throw new Error('Dados salvos. Não foi possível atualizar o prazo; tente novamente.');
     const pagina=new DOMParser().parseFromString(await respostaPrazo.text(),'text/html');
     const novo=pagina.querySelector('#pedidoPrazo'),atual=document.querySelector('#pedidoPrazo');
     if(!novo||!atual)throw new Error('Dados salvos. Reabra o orçamento para conferir o prazo.');
     if(edicoes.get(form)!==edicao)return;
     atual.replaceChildren(...novo.childNodes);document.dispatchEvent(new Event('pedido:preview'));
     const prazo=pagina.querySelector('[data-pedido-barra-valor]');if(prazo)document.querySelectorAll('[data-valor-total="false"]').forEach(no=>no.textContent=prazo.textContent);
    }
   }catch(erro){falhas.set(form,erro.message);sujos.add(form);}
   finally{pendentes--;status(falhas.size?[...falhas.values()][0]:pendentes?'Salvando…':'Alterações salvas',falhas.size>0);}
  });return fila;
 }
 document.addEventListener('input',e=>{
  if(e.target.matches?.('[data-pedido-titulo]')){const form=e.target.closest('form'),titulo=form.querySelector('[name="titulo"]');titulo.value=e.target.value;}
  const form=formDo(e.target);if(form){sujos.add(form);edicoes.set(form,(edicoes.get(form)||0)+1);
   if(form.dataset.etapa==='2'&&['inicioPretendido','turnoInicio','turnoFim','dias','intervalo','folga'].includes(e.target.name)){
    clearTimeout(temporizadores.get(form));temporizadores.set(form,setTimeout(()=>{if(form.isConnected&&formularioPronto(form)){automaticos.add(form);form.requestSubmit();}},400));
   }
  }
 });
 document.addEventListener('change',e=>{const form=formDo(e.target);if(form?.dataset.autosaveUrl&&formularioPronto(form)){clearTimeout(temporizadores.get(form));automaticos.add(form);form.requestSubmit();}});
 async function terminar(){
  do {
   for(const form of [...sujos]){if(!form.isConnected){sujos.delete(form);continue;}if(!form.checkValidity()){form.reportValidity();return false;}salvar(form);}
   await fila;if(falhas.size)return false;
  }while(sujos.size||pendentes);
  return true;
 }
 document.addEventListener('submit',e=>{
  const form=e.target;if(!form.closest?.('.pagina-pedidos'))return;
  if(automaticos.has(form)){e.preventDefault();e.stopImmediatePropagation();automaticos.delete(form);salvar(form);return;}
  if(liberados.has(form)){liberados.delete(form);return;}
  if(!pendentes&&!sujos.size&&!falhas.size)return;
  e.preventDefault();e.stopImmediatePropagation();const botao=e.submitter;
  terminar().then(ok=>{if(ok&&form.isConnected){liberados.add(form);form.requestSubmit(botao);}});
 },true);
 document.addEventListener('click',e=>{
  if(e.target.closest?.('[data-pedido-retry]')){terminar();return;}
  const link=e.target.closest?.('.pagina-pedidos a[href]');if(!link||e.ctrlKey||e.metaKey||e.shiftKey||e.altKey||link.getAttribute('href')?.startsWith('#'))return;
  if(!pendentes&&!sujos.size&&!falhas.size)return;
  e.preventDefault();e.stopImmediatePropagation();terminar().then(ok=>{if(ok)link.click();});
 },true);
 window.addEventListener?.('beforeunload',e=>{if(pendentes||sujos.size||falhas.size){e.preventDefault();e.returnValue='';}});
})();
