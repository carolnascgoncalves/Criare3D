(() => {
 const inicializados = new WeakSet();
 function iniciar() {
  const painel = document.querySelector('[data-simulacao]');
  const botao = painel?.querySelector('[data-salvar-planejamento]');
  if (!botao || inicializados.has(botao)) return;
  inicializados.add(botao);
  botao.addEventListener('click', evento => {
   evento.preventDefault();evento.stopPropagation();
   const form = document.getElementById('salvarPlanejamento');
   const campos = {calculoId:'calculo',unidades:'unidades',capacidade:'por-impressao',inicio:'inicio',intervalo:'intervalo',folga:'folga'};
   const valores = Object.fromEntries(Object.entries(campos).map(([nome,chave]) => [nome,painel.querySelector(`[data-sim="${chave}"]`).value]));
   if (!valores.calculoId || !valores.inicio || Number(valores.unidades)<1 || Number(valores.capacidade)<1 || Number(valores.capacidade)>Number(valores.unidades)) {
    window.mostrarNotificacao?.('Escolha um cálculo, confira as quantidades e informe o início.');return;
   }
   form.querySelectorAll('[data-planejamento-campo]').forEach(el => el.remove());
   for(const [nome,valor] of Object.entries(valores)) {const input=document.createElement('input');input.type='hidden';input.name=nome;input.value=valor;input.dataset.planejamentoCampo='true';form.appendChild(input);}
   botao.disabled=true; form.requestSubmit();
  });
 }
 document.addEventListener('DOMContentLoaded',iniciar);document.addEventListener('turbo:load',iniciar);
 document.addEventListener('turbo:submit-end',evento=>{if(evento.target.id==='salvarPlanejamento'){const b=document.querySelector('[data-salvar-planejamento]');if(b)b.disabled=false;}});
 document.addEventListener('turbo:before-cache',()=>{const b=document.querySelector('[data-salvar-planejamento]');if(b) b.disabled=false;});
})();
