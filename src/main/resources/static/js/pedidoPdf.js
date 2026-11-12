(() => {
 if(window.criarePedidoPdf)return;window.criarePedidoPdf=true;
 const arquivos=new WeakMap();
 document.addEventListener('click',async e=>{
  if(e.target.closest('[data-pedido-abrir-pdf]')){const caixa=document.querySelector('[data-pedido-pdf]');if(caixa){caixa.hidden=false;caixa.querySelector('input').focus();}return;}
  const botao=e.target.closest('[data-pedido-pdf-enviar]');if(!botao)return;
  const caixa=botao.closest('[data-pedido-pdf]'),arquivo=arquivos.get(caixa),status=caixa.querySelector('[data-pedido-pdf-status]');
  if(!arquivo)return;
  if(!navigator.share||!navigator.canShare?.({files:[arquivo]})){status.textContent='Este navegador não compartilha arquivos. Abra a conversa pelo botão de WhatsApp e anexe o PDF salvo usando o clipe de documentos.';return;}
  try{await navigator.share({files:[arquivo],title:'Proposta em PDF'});status.textContent='Compartilhamento concluído. Confira a conversa antes de marcar a proposta como enviada.';}
  catch(erro){if(erro.name!=='AbortError')status.textContent='Não foi possível compartilhar. Use o clipe de documentos na conversa para anexar o PDF salvo.';}
 });
 document.addEventListener('change',e=>{
  if(!e.target.matches('[data-pedido-pdf-arquivo]'))return;
  const caixa=e.target.closest('[data-pedido-pdf]'),arquivo=e.target.files?.[0],status=caixa.querySelector('[data-pedido-pdf-status]');
  const valido=arquivo&&arquivo.type==='application/pdf'&&arquivo.size>0&&arquivo.size<=20*1024*1024;
  caixa.querySelector('[data-pedido-pdf-enviar]').disabled=!valido;
  if(valido){arquivos.set(caixa,arquivo);status.textContent='PDF selecionado. Clique em compartilhar e escolha o aplicativo e o destinatário.';}
  else{arquivos.delete(caixa);status.textContent='Selecione um PDF de até 20 MB.';}
 });
 document.addEventListener('turbo:before-cache',()=>document.querySelectorAll('[data-pedido-pdf]').forEach(caixa=>{arquivos.delete(caixa);caixa.hidden=true;caixa.querySelector('input').value='';caixa.querySelector('[data-pedido-pdf-enviar]').disabled=true;}));
})();
