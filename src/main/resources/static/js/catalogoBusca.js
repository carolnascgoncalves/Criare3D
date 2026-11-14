(() => {
 if(window.catalogoBuscaAtivo)return;window.catalogoBuscaAtivo=true;
 let observador;
 function inicializar(){
  document.querySelectorAll('[data-modo-custos]').forEach(modo=>{
   if(modo.dataset.modoPronto)return;modo.dataset.modoPronto='true';
   const campo=modo.closest('form')?.querySelector('[data-embalagem-recalculo]');if(!campo)return;
   const atualizar=()=>{campo.hidden=modo.value!=='true';campo.querySelector('select').disabled=campo.hidden;};modo.addEventListener('change',atualizar);atualizar();
  });
  document.querySelectorAll('select[data-catalogo]').forEach(select=>{
   if(select.dataset.buscaCatalogo)return;select.dataset.buscaCatalogo='true';
   const abrir=document.createElement('button');abrir.type='button';abrir.className='botao botao-secundario catalogo-buscar';abrir.textContent='Buscar no catálogo';
   abrir.addEventListener('click',()=>{if(!select.disabled)abrirBusca(select);});select.insertAdjacentElement('afterend',abrir);
  });
 }
 function abrirBusca(select){
  const dialogo=document.createElement('dialog');dialogo.className='catalogo-dialogo';dialogo.setAttribute('aria-label','Buscar no catálogo');
  const titulo=document.createElement('h2');titulo.textContent='Buscar no catálogo';
  const busca=document.createElement('input');busca.type='search';busca.placeholder='Digite o nome';busca.setAttribute('aria-label','Buscar pelo nome');
  const estado=document.createElement('p');estado.setAttribute('role','status');
  const lista=document.createElement('div');lista.className='catalogo-resultados';
  const mais=document.createElement('button');mais.type='button';mais.className='botao botao-secundario';mais.textContent='Carregar mais';
  const fechar=document.createElement('button');fechar.type='button';fechar.className='botao botao-secundario';fechar.textContent='Fechar';fechar.addEventListener('click',()=>dialogo.close());
  dialogo.append(titulo,busca,estado,lista,mais,fechar);document.body.append(dialogo);
  let pagina=0,controle,timer,versao=0;
  async function carregar(acrescentar=false){
   controle?.abort();controle=new AbortController();const atual=++versao;estado.textContent='Buscando…';mais.disabled=true;
   try{
    const params=new URLSearchParams({tipo:select.dataset.catalogo,busca:busca.value,page:String(pagina)});
    const resposta=await fetch('/catalogo-opcoes?'+params,{signal:controle.signal,headers:{Accept:'application/json'}});
    if(!resposta.ok)throw new Error('Não foi possível buscar. Tente novamente.');
    const dados=await resposta.json();if(atual!==versao)return;if(!acrescentar)lista.replaceChildren();
    dados.itens.forEach(item=>{const b=document.createElement('button');b.type='button';b.className='catalogo-resultado';b.textContent=item.nome;b.addEventListener('click',()=>{
     let opcao=[...select.options].find(o=>o.value===String(item.id));if(!opcao){opcao=document.createElement('option');opcao.value=item.id;select.append(opcao);}
     opcao.textContent=item.nome;Object.assign(opcao.dataset,item.atributos);if(select.closest('[data-calculo-rapido]')&&select.dataset.catalogo==='filamento')opcao.dataset.cor=item.atributos.codigoCor||'';select.value=item.id;
     select.dispatchEvent(new Event('change',{bubbles:true}));dialogo.close();
    });lista.append(b);});
    estado.textContent=lista.children.length?'Escolha um resultado.':'Nenhum resultado encontrado.';mais.hidden=!dados.mais;
   }catch(e){if(e.name!=='AbortError'&&atual===versao)estado.textContent=e.message;}finally{if(atual===versao)mais.disabled=false;}
  }
  busca.addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(()=>{pagina=0;carregar();},300);});
  mais.addEventListener('click',()=>{pagina++;carregar(true);});
  dialogo.addEventListener('close',()=>{clearTimeout(timer);controle?.abort();dialogo.remove();abrirFoco();},{once:true});
  function abrirFoco(){select.parentElement?.querySelector('.catalogo-buscar')?.focus();}
  dialogo.showModal();busca.focus();carregar();
 }
 function carregar(){inicializar();observador?.disconnect();observador=new MutationObserver(()=>inicializar());observador.observe(document.body,{childList:true,subtree:true});}
 document.addEventListener('change',event=>{
  const select=event.target;if(!select.matches?.('[data-comprador-orcamento]'))return;
  const opcao=select.selectedOptions[0];if(!select.value)return;
  for(const [nome,valor] of [['cliente',opcao.textContent],['contato',opcao.dataset.telefone||''],['email',opcao.dataset.email||'']]){
   const input=document.querySelector('[name="'+nome+'"]');if(input){input.value=valor;input.dispatchEvent(new Event('input',{bubbles:true}));}
  }
 });
 document.addEventListener('DOMContentLoaded',carregar);document.addEventListener('turbo:load',carregar);
 document.addEventListener('turbo:before-cache',()=>{observador?.disconnect();document.querySelectorAll('.catalogo-dialogo').forEach(d=>d.close());document.querySelectorAll('select[data-busca-catalogo]').forEach(s=>delete s.dataset.buscaCatalogo);document.querySelectorAll('[data-modo-pronto]').forEach(s=>delete s.dataset.modoPronto);document.querySelectorAll('.catalogo-buscar').forEach(b=>b.remove());});
})();
