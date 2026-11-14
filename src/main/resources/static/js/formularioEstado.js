(() => {
 if(window.formularioEstadoAtivo)return;window.formularioEstadoAtivo=true;
 document.addEventListener('turbo:submit-start',evento=>{
  const form=evento.detail?.formSubmission?.formElement||evento.target;
  if(!form?.matches?.('form')||form.dataset.autosaveUrl)return;
  form.setAttribute('aria-busy','true');
  let status=form.querySelector('[data-envio-status]');
  if(!status){status=document.createElement('p');status.dataset.envioStatus='true';status.className='texto-apoio';status.setAttribute('role','status');form.append(status);}
  status.textContent='Enviando… Aguarde a confirmação.';
 });
 document.addEventListener('turbo:submit-end',evento=>{
  const form=evento.detail?.formSubmission?.formElement||evento.target;
  if(!form?.matches?.('form')||form.dataset.autosaveUrl)return;
  form.removeAttribute('aria-busy');
  const status=form.querySelector('[data-envio-status]');
  if(status)status.textContent=evento.detail?.success?'':'Não foi possível concluir. Confira os campos e tente novamente.';
 });
 document.addEventListener('turbo:before-cache',()=>{
  document.querySelectorAll('form[aria-busy]').forEach(f=>f.removeAttribute('aria-busy'));
  document.querySelectorAll('[data-envio-status]').forEach(e=>e.remove());
 });
})();
