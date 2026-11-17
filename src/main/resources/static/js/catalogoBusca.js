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
   select.dataset.valorCatalogo=select.value;
   abrir.addEventListener('click',()=>{if(!select.disabled)abrirBusca(select);});select.insertAdjacentElement('afterend',abrir);atualizarComprador(select);if(seletorUnico(select)){select.classList.add('catalogo-seletor');atualizarSeletor(select);select.addEventListener('change',()=>{if(!select.selectedOptions[0]?.dataset.acao)select.dataset.valorCatalogo=select.value;atualizarSeletor(select);});}
  });
 }
 const numero=(v,max=4)=>new Intl.NumberFormat('pt-BR',{maximumFractionDigits:max}).format(Number(v));
 const moeda=v=>'R$ '+Number(v).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:4});
 function cadastroNoCalculo(select){
  if(!select.closest('#formCalculo,[data-calculo-rapido]'))return null;
  const rotulos={filamento:'Cadastrar novo filamento',impressora:'Cadastrar nova impressora',embalagem:'Cadastrar nova embalagem'};
  if(!rotulos[select.dataset.catalogo])return null;
  const botao=document.createElement('button');botao.type='button';
  botao.className='botao botao-principal';botao.textContent=rotulos[select.dataset.catalogo];
  return botao;
 }
 function selecionarItem(select,item){
  let opcao=[...select.options].find(o=>o.value===String(item.id));
  if(!opcao){opcao=document.createElement('option');opcao.value=String(item.id);select.append(opcao);}
  opcao.textContent=item.nome;Object.assign(opcao.dataset,item.atributos||{});
  if(select.closest('[data-calculo-rapido]')&&select.dataset.catalogo==='filamento')opcao.dataset.cor=item.atributos.codigoCor||'';
  select.value=String(item.id);select.dispatchEvent(new Event('change',{bubbles:true}));
 }
 let sequenciaCadastro=0;
 function formularioCadastro(select,aoSalvar,aoCancelar){
  const tipo=select.dataset.catalogo;
  const campos={
   filamento:[['nome','Nome do filamento','text',2,150],['cor','Nome da cor','text',2,80],['codigoCor','Escolha a cor','color'],['pesoRoloGramas','Peso do rolo (g)','number','0.01'],['precoRolo','Preço do rolo (R$)','number','0.01'],['estoqueGramas','Quantidade disponível (g)','number','0']],
   impressora:[['nome','Nome da impressora','text',2,150],['modelo','Modelo','select'],['modeloPersonalizado','Nome do outro modelo','text',2,150],['potenciaWatts','Potência média (W)','number','0']],
   embalagem:[['nome','Nome da embalagem','text',2,150],['custoUnitario','Custo unitário (R$)','number','0.01']]
  }[tipo];
  const form=document.createElement('form');form.className='formulario';form.method='post';form.noValidate=true;
  form.dataset.cadastroCatalogo=tipo;form.dataset.turbo='false';
  const prefixo='cadastro-catalogo-'+(++sequenciaCadastro)+'-';
  const erro=document.createElement('p');erro.className='alerta-erro';erro.hidden=true;erro.setAttribute('role','alert');
  form.append(erro);
  const inputs=new Map();
  campos.forEach(([nome,rotulo,tipoCampo,minimo,maximo])=>{
   const grupo=document.createElement('div');grupo.className='campo';
   const label=document.createElement('label');label.htmlFor=prefixo+nome;label.textContent=rotulo;
   const input=document.createElement(tipoCampo==='select'?'select':'input');input.id=prefixo+nome;input.name=nome;if(tipoCampo!=='select')input.type=tipoCampo;input.required=true;
   if(tipoCampo==='text'){input.minLength=minimo;input.maxLength=maximo;}
   if(tipoCampo==='number'){input.min=minimo;input.step='0.01';input.inputMode='decimal';}
   if(nome==='codigoCor')input.value='#75c9a5';
   if(nome==='pesoRoloGramas')input.value='1000';
   if(nome==='estoqueGramas')input.value='0';
   const mensagem=document.createElement('small');mensagem.className='erro-campo';mensagem.dataset.erroApresentado='true';mensagem.id=prefixo+nome+'-erro';mensagem.hidden=true;
   input.setAttribute('aria-describedby',mensagem.id);
   input.addEventListener('input',()=>{input.removeAttribute('aria-invalid');mensagem.hidden=true;});
   inputs.set(nome,{input,mensagem,grupo});grupo.append(label,input,mensagem);form.append(grupo);
  });
  const nota=document.createElement('p');nota.className='texto-apoio';
  nota.textContent=tipo==='filamento'?'Informe a quantidade que você tem disponível agora, em gramas. A marca pode ser preenchida depois em Filamentos.':'Você pode completar os demais dados depois na tela de cadastro.';
  let modelosCarregando=false;
  if(tipo==='impressora'){
   const modelo=inputs.get('modelo').input;
   const opcao=(nome,valor)=>{const elemento=document.createElement('option');elemento.textContent=nome;elemento.value=valor;return elemento;};
   let referencias=[];
   function atualizarModelo(){
    const manual=modelo.value==='__outro__';
    for(const nome of ['modeloPersonalizado','potenciaWatts']){const campo=inputs.get(nome);campo.grupo.hidden=!manual;campo.input.disabled=!manual;}
    const referencia=referencias.find(item=>item.nome===modelo.value);
    nota.textContent=referencia?`Potência média de referência para PLA: ${referencia.potenciaMediaPLA} W. Você pode ajustar depois em Impressoras.`:manual?'Informe a potência média medida ou estimada para este modelo.':'Escolha um modelo para preencher a potência automaticamente.';
   }
   modelo.addEventListener('change',atualizarModelo);
   modelo.append(opcao('Carregando modelos…',''));modelo.disabled=true;modelosCarregando=true;atualizarModelo();
   Promise.resolve().then(()=>window.carregarModelosImpressora()).then(dados=>{
    referencias=dados;modelo.replaceChildren(opcao('Selecione o modelo',''),...dados.map(item=>opcao(item.nome,item.nome)),opcao('Outro modelo','__outro__'));
   }).catch(()=>{
    modelo.replaceChildren(opcao('Selecione o modelo',''),opcao('Outro modelo','__outro__'));
    mostrarErros('Não foi possível carregar as referências. Escolha Outro modelo para informar os dados manualmente.');
   }).finally(()=>{modelo.disabled=false;modelosCarregando=false;});
  }
  const acoes=document.createElement('div');acoes.className='operacao-acoes';
  const salvar=document.createElement('button');salvar.type='submit';salvar.className='botao botao-principal';salvar.textContent='Salvar e selecionar';
  const cancelar=document.createElement('button');cancelar.type='button';cancelar.className='botao botao-secundario';cancelar.textContent='Cancelar';
  let salvando=false;
  cancelar.addEventListener('click',()=>{if(!salvando)aoCancelar();});
  acoes.append(salvar,cancelar);form.append(nota,acoes);
  function mostrarErros(mensagem,camposInvalidos={}){
   erro.textContent=mensagem;erro.hidden=false;
   Object.entries(camposInvalidos).forEach(([nome,texto])=>{
    const campo=inputs.get(nome);if(!campo)return;
    campo.input.setAttribute('aria-invalid','true');campo.mensagem.textContent=texto;campo.mensagem.hidden=false;
   });
   const primeiro=[...inputs.values()].find(campo=>!campo.mensagem.hidden);primeiro?.input.focus();
  }
  form.addEventListener('submit',async evento=>{
   evento.preventDefault();if(salvando)return;
   if(modelosCarregando){mostrarErros('Aguarde o carregamento dos modelos.');return;}
   erro.hidden=true;const invalidos={};const dados={};
   inputs.forEach(({input,mensagem},nome)=>{
    if(input.disabled)return;
    input.removeAttribute('aria-invalid');mensagem.hidden=true;
    if(!input.validity.valid)invalidos[nome]=input.validationMessage;
    dados[nome]=input.type==='number'?(input.value===''?null:input.value):input.value.trim();
   });
   if(tipo==='impressora'&&dados.modelo==='__outro__'){dados.modelo=dados.modeloPersonalizado;delete dados.modeloPersonalizado;}
   if(Object.keys(invalidos).length){mostrarErros('Confira os campos destacados.',invalidos);return;}
   salvando=true;salvar.disabled=true;cancelar.disabled=true;salvar.textContent='Salvando…';form.setAttribute('aria-busy','true');
   try{
    const tokens=document.querySelectorAll('meta[name="csrf-token"]');const token=tokens[tokens.length-1]?.content;
    const resposta=await fetch('/catalogo-opcoes/cadastro/'+tipo,{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json',Accept:'application/json','X-CSRF-TOKEN':token||''},body:JSON.stringify(dados)});
    if(resposta.redirected||resposta.status===401||resposta.status===403)throw new Error('Sua sessão pode ter expirado. Entre novamente para cadastrar.');
    if(!resposta.headers.get('Content-Type')?.includes('application/json'))throw new Error('Não foi possível salvar o cadastro. Tente novamente.');
    const corpo=await resposta.json();
    if(!resposta.ok){mostrarErros(corpo.mensagem||'Não foi possível cadastrar. Confira os dados.',corpo.campos);return;}
    if(!corpo.id)throw new Error('Não foi possível confirmar o cadastro. Confira o catálogo antes de tentar novamente.');
    aoSalvar(corpo);
   }catch(e){mostrarErros(e.message||'Não foi possível cadastrar. Tente novamente.');}
   finally{salvando=false;salvar.disabled=false;cancelar.disabled=false;salvar.textContent='Salvar e selecionar';form.removeAttribute('aria-busy');}
  });
  return {form,primeiro:[...inputs.values()][0].input,salvando:()=>salvando};
 }
 function seletorUnico(select){return select.dataset.catalogo==='filamento'||(['impressora','embalagem','produto'].includes(select.dataset.catalogo)&&!!select.closest('#formCalculo,[data-calculo-rapido],.pagina-outras-vendas'));}
 function atualizarSeletor(select){const b=select.nextElementSibling;if(!b?.classList.contains('catalogo-buscar'))return;const rotulos={filamento:'Selecionar filamento',impressora:'Selecionar impressora',embalagem:'Selecionar embalagem',produto:'Selecionar produto'};const texto=select.value?select.selectedOptions[0].textContent.split(' · R$')[0]:rotulos[select.dataset.catalogo];if(b.textContent!==texto)b.textContent=texto;if(b.disabled!==select.disabled)b.disabled=select.disabled;}

 function apresentarResultado(botao,item,tipo){
  const a=item.atributos||{};botao.setAttribute('aria-label',item.nome);const icone=document.createElement('i');icone.className='bi '+({filamento:'bi-disc',impressora:'bi-printer',peca:'bi-box',embalagem:'bi-box-seam',produto:'bi-bag',comprador:'bi-person'}[tipo]||'bi-search');icone.setAttribute('aria-hidden','true');
  if(tipo==='filamento'){const cores={preto:'#242424',branco:'#ffffff',vermelho:'#dd3535',amarelo:'#f4d744',azul:'#447ed4',verde:'#499365',marrom:'#986c50','marrom claro':'#bd9b87',dourado:'#d6b345'};icone.className='catalogo-cor';icone.style.backgroundColor=/^#[a-f0-9]{6}$/i.test(a.codigoCor||'')?a.codigoCor:(cores[(a.cor||'').toLowerCase()]||'#ddd8e6');icone.title=a.cor||'Cor não informada';}
  const textos=document.createElement('span');const nome=document.createElement('strong');nome.textContent=item.nome;const detalhe=document.createElement('small');
  detalhe.textContent=tipo==='filamento'?(Number(a.pesoRolo)>0?moeda(Number(a.precoRolo)/Number(a.pesoRolo))+'/g':'Preço por grama não informado')+(a.estoque!=null?' · '+numero(a.estoque)+' g disponíveis':''):tipo==='impressora'?[a.modelo,a.potencia!=null?numero(a.potencia)+' W':''].filter(Boolean).join(' · '):tipo==='peca'?[a.gramas!=null?numero(a.gramas)+' g':'',a.tempo!=null?numero(a.tempo)+' min':''].filter(Boolean).join(' · '):tipo==='comprador'?[a.telefone,a.email].filter(Boolean).join(' · '):a.custo!=null?moeda(a.custo):a.preco!=null?moeda(a.preco):'';
  textos.append(nome,detalhe);botao.replaceChildren(icone,textos);
 }
 function abrirBusca(select,iniciarCadastro=false){
  const dialogo=document.createElement('dialog');dialogo.className='catalogo-dialogo';dialogo.setAttribute('aria-label','Buscar no catálogo');
  const titulo=document.createElement('h2');titulo.textContent={filamento:'Buscar filamento',impressora:'Buscar impressora',peca:'Buscar peça',embalagem:'Buscar embalagem',produto:'Buscar produto',comprador:'Buscar cliente'}[select.dataset.catalogo]||'Buscar no catálogo';
  const busca=document.createElement('input');busca.type='search';busca.placeholder=select.dataset.catalogo==='filamento'?'Digite o material ou a cor':'Digite o nome';busca.setAttribute('aria-label','Buscar pelo nome');
  const estado=document.createElement('p');estado.className='catalogo-estado';estado.setAttribute('role','status');
  const lista=document.createElement('div');lista.className='catalogo-resultados';
  const mais=document.createElement('button');mais.type='button';mais.className='botao botao-secundario';mais.textContent='Carregar mais';
  const fechar=document.createElement('button');fechar.type='button';fechar.className='botao botao-secundario';fechar.textContent='Fechar';fechar.addEventListener('click',()=>dialogo.close());
  const cadastrar=cadastroNoCalculo(select);
  const acoes=document.createElement('div');acoes.className='operacao-acoes';
  acoes.append(mais);if(cadastrar)acoes.append(cadastrar);acoes.append(fechar);
  dialogo.append(titulo,busca,estado,lista,acoes);document.body.append(dialogo);
  let pagina=0,controle,timer,versao=0;
  let painelCadastro;
  cadastrar?.addEventListener('click',()=>{
   if(painelCadastro)return;
   controle?.abort();versao++;clearTimeout(timer);
   [busca,estado,lista,acoes].forEach(elemento=>elemento.hidden=true);
   titulo.textContent={filamento:'Novo filamento',impressora:'Nova impressora',embalagem:'Nova embalagem'}[select.dataset.catalogo];
   dialogo.setAttribute('aria-label',titulo.textContent);
   painelCadastro=formularioCadastro(select,item=>{selecionarItem(select,item);dialogo.close();},()=>dialogo.close());
   dialogo.append(painelCadastro.form);painelCadastro.primeiro.focus();
  });
  dialogo.addEventListener('cancel',evento=>{if(painelCadastro?.salvando())evento.preventDefault();});
  async function carregar(acrescentar=false){
   controle?.abort();controle=new AbortController();const atual=++versao;estado.textContent='Buscando…';mais.disabled=true;
   try{
    const params=new URLSearchParams({tipo:select.dataset.catalogo,busca:busca.value,page:String(pagina)});
    const resposta=await fetch('/catalogo-opcoes?'+params,{signal:controle.signal,headers:{Accept:'application/json'}});
    if(!resposta.ok)throw new Error('Não foi possível buscar. Tente novamente.');
    const dados=await resposta.json();if(atual!==versao)return;if(!acrescentar)lista.replaceChildren();
    dados.itens.forEach(item=>{const b=document.createElement('button');b.type='button';b.className='catalogo-resultado';b.textContent=item.nome;apresentarResultado(b,item,select.dataset.catalogo);b.addEventListener('click',()=>{
     selecionarItem(select,item);dialogo.close();
    });lista.append(b);});
    estado.textContent=lista.children.length?'Escolha um resultado.':'Nenhum resultado encontrado.';mais.hidden=!dados.mais;
   }catch(e){if(e.name!=='AbortError'&&atual===versao)estado.textContent=e.message;}finally{if(atual===versao)mais.disabled=false;}
  }
  busca.addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(()=>{pagina=0;carregar();},300);});
  mais.addEventListener('click',()=>{pagina++;carregar(true);});
  dialogo.addEventListener('close',()=>{clearTimeout(timer);controle?.abort();dialogo.remove();abrirFoco();},{once:true});
  function abrirFoco(){select.parentElement?.querySelector('.catalogo-buscar')?.focus();}
  dialogo.showModal();busca.focus();if(iniciarCadastro&&cadastrar)cadastrar.click();else carregar();
 }
 window.abrirCadastroCatalogo=select=>{
  if(!select||select.disabled||!select.closest('#formCalculo,[data-calculo-rapido]')||!['filamento','impressora','embalagem'].includes(select.dataset.catalogo))return false;
  if(select.selectedOptions[0]?.dataset.acao)select.value=select.dataset.valorCatalogo||'';
  abrirBusca(select,true);return true;
 };
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
