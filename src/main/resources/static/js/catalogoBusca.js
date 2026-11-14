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
   if(select.dataset.buscaCatalogo){if(seletorUnico(select))atualizarSeletor(select);return;}select.dataset.buscaCatalogo='true';
   const abrir=document.createElement('button');abrir.type='button';abrir.className='botao botao-secundario catalogo-buscar';abrir.textContent=select.matches('[data-comprador-orcamento]')?'Buscar cliente':'Buscar no catálogo';
   abrir.addEventListener('click',()=>{if(!select.disabled)abrirBusca(select);});select.insertAdjacentElement('afterend',abrir);atualizarComprador(select);if(seletorUnico(select)){select.classList.add('catalogo-seletor');atualizarSeletor(select);select.addEventListener('change',()=>atualizarSeletor(select));}
  });
 }
 const numero=(v,max=4)=>new Intl.NumberFormat('pt-BR',{maximumFractionDigits:max}).format(Number(v));
 const moeda=v=>'R$ '+Number(v).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:4});
 function seletorUnico(select){return select.dataset.catalogo==='filamento'||(['impressora','embalagem','produto'].includes(select.dataset.catalogo)&&!!select.closest('#formCalculo,[data-calculo-rapido],.pagina-outras-vendas'));}
 function atualizarSeletor(select){const b=select.nextElementSibling;if(!b?.classList.contains('catalogo-buscar'))return;const rotulos={filamento:'Selecionar filamento',impressora:'Selecionar impressora',embalagem:'Selecionar embalagem',produto:'Selecionar produto'};const texto=select.value?select.selectedOptions[0].textContent.split(' · R$')[0]:rotulos[select.dataset.catalogo];if(b.textContent!==texto)b.textContent=texto;if(b.disabled!==select.disabled)b.disabled=select.disabled;}

 function apresentarResultado(botao,item,tipo){
  const a=item.atributos||{};botao.setAttribute('aria-label',item.nome);const icone=document.createElement('i');icone.className='bi '+({filamento:'bi-disc',impressora:'bi-printer',peca:'bi-box',embalagem:'bi-box-seam',produto:'bi-bag',comprador:'bi-person'}[tipo]||'bi-search');icone.setAttribute('aria-hidden','true');
  if(tipo==='filamento'){const cores={preto:'#242424',branco:'#ffffff',vermelho:'#dd3535',amarelo:'#f4d744',azul:'#447ed4',verde:'#499365',marrom:'#986c50','marrom claro':'#bd9b87',dourado:'#d6b345'};icone.className='catalogo-cor';icone.style.backgroundColor=/^#[a-f0-9]{6}$/i.test(a.codigoCor||'')?a.codigoCor:(cores[(a.cor||'').toLowerCase()]||'#ddd8e6');icone.title=a.cor||'Cor não informada';}
  const textos=document.createElement('span');const nome=document.createElement('strong');nome.textContent=item.nome;const detalhe=document.createElement('small');
  detalhe.textContent=tipo==='filamento'?(Number(a.pesoRolo)>0?moeda(Number(a.precoRolo)/Number(a.pesoRolo))+'/g':'Preço por grama não informado')+(a.estoque!=null?' · '+numero(a.estoque)+' g disponíveis':''):tipo==='impressora'?[a.modelo,a.potencia!=null?numero(a.potencia)+' W':''].filter(Boolean).join(' · '):tipo==='peca'?[a.gramas!=null?numero(a.gramas)+' g':'',a.tempo!=null?numero(a.tempo)+' min':''].filter(Boolean).join(' · '):tipo==='comprador'?[a.telefone,a.email].filter(Boolean).join(' · '):a.custo!=null?moeda(a.custo):a.preco!=null?moeda(a.preco):'';
  textos.append(nome,detalhe);botao.replaceChildren(icone,textos);
 }
 function abrirBusca(select){
  const dialogo=document.createElement('dialog');dialogo.className='catalogo-dialogo';dialogo.setAttribute('aria-label','Buscar no catálogo');
  const titulo=document.createElement('h2');titulo.textContent={filamento:'Buscar filamento',impressora:'Buscar impressora',peca:'Buscar peça',embalagem:'Buscar embalagem',produto:'Buscar produto',comprador:'Buscar cliente'}[select.dataset.catalogo]||'Buscar no catálogo';
  const busca=document.createElement('input');busca.type='search';busca.placeholder=select.dataset.catalogo==='filamento'?'Digite o material ou a cor':'Digite o nome';busca.setAttribute('aria-label','Buscar pelo nome');
  const estado=document.createElement('p');estado.className='catalogo-estado';estado.setAttribute('role','status');
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
    dados.itens.forEach(item=>{const b=document.createElement('button');b.type='button';b.className='catalogo-resultado';b.textContent=item.nome;apresentarResultado(b,item,select.dataset.catalogo);b.addEventListener('click',()=>{
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
 function carregar(){inicializar();observador?.disconnect();observador=new MutationObserver(()=>inicializar());observador.observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['disabled']});}
 function atualizarComprador(select){
  if(!select.matches?.('[data-comprador-orcamento]'))return;
  const bloco=select.closest('.comprador-escolha');if(!bloco)return;
  const texto=select.value?select.selectedOptions[0]?.textContent:'Preencha os dados abaixo ou busque um cliente';
  const resumo=bloco.querySelector('[data-cliente-selecionado]');if(resumo&&resumo.textContent!==texto)resumo.textContent=texto;
  const botao=bloco.querySelector('.catalogo-buscar');const rotulo=select.value?'Trocar cliente':'Buscar cliente';if(botao&&botao.textContent!==rotulo)botao.textContent=rotulo;
  const manual=bloco.querySelector('[data-comprador-limpar]');if(manual)manual.hidden=!select.value;
 }
 document.addEventListener('click',event=>{
  const manual=event.target.closest?.('[data-comprador-limpar]');if(!manual)return;
  const select=manual.closest('.comprador-escolha')?.querySelector('[data-comprador-orcamento]');if(!select)return;
  select.value='';select.dispatchEvent(new Event('change',{bubbles:true}));document.querySelector('[name="cliente"]')?.focus();
 });
 document.addEventListener('change',event=>{
  const select=event.target;if(!select.matches?.('[data-comprador-orcamento]'))return;
  atualizarComprador(select);const opcao=select.selectedOptions[0];if(!select.value)return;
  for(const [nome,valor] of [['cliente',opcao.textContent],['contato',opcao.dataset.telefone||''],['email',opcao.dataset.email||'']]){
   const input=document.querySelector('[name="'+nome+'"]');if(input){input.value=valor;input.dispatchEvent(new Event('input',{bubbles:true}));}
  }
 });
 document.addEventListener('DOMContentLoaded',carregar);document.addEventListener('turbo:load',carregar);
 document.addEventListener('turbo:before-cache',()=>{observador?.disconnect();document.querySelectorAll('.catalogo-dialogo').forEach(d=>d.close());document.querySelectorAll('select[data-busca-catalogo]').forEach(s=>delete s.dataset.buscaCatalogo);document.querySelectorAll('[data-modo-pronto]').forEach(s=>delete s.dataset.modoPronto);document.querySelectorAll('.catalogo-buscar').forEach(b=>b.remove());});
})();
