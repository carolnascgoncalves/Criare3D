// Os totais do arquivo só podem completar dados de uma placa quando há uma única placa.
function prepararPlacasConsultaRapida(modelo) {
    const placas = modelo.placas?.length ? modelo.placas : [{numero: 1}];
    const unica = placas.length === 1 && !(modelo.numeroPlacas > 1);
    return placas.map(placa => ({
        ...placa,
        filamentos: placa.filamentos?.length ? placa.filamentos : (unica ? modelo.filamentosUsados || [] : []),
        tempoSegundos: placa.tempoSegundos ?? (unica ? modelo.tempoTotalSegundos : null),
        pesoGramas: placa.pesoGramas ?? (unica ? modelo.pesoTotalGramas : null)
    }));
}

function resumirArquivoConsultaRapida(modelo) {
    const placas=prepararPlacasConsultaRapida(modelo);
    const numeroPlacas=Number(modelo.numeroPlacas) || placas.length;
    const todas=placas.length===numeroPlacas;
    const quantidadeCompleta=todas && placas.every(p=>Number.isInteger(p.quantidadePecas) && p.quantidadePecas>0);
    const tempoCompleto=(todas && placas.every(p=>Number(p.tempoSegundos)>0)) || modelo.estatisticasCompletas===true;
    const soma=new Map();
    const materiais=modelo.filamentosUsados?.length ? modelo.filamentosUsados : placas.flatMap(p=>p.filamentos || []);
    for(const f of materiais) {
        const chave=`${f.id}|${f.material || ''}|${f.cor || ''}`;
        const anterior=soma.get(chave);
        soma.set(chave,{...f,pesoGramas:Number(f.pesoGramas || 0)+(anterior?.pesoGramas || 0)});
    }
    return {placas,numeroPlacas,quantidadePecas:quantidadeCompleta ? placas.reduce((s,p)=>s+p.quantidadePecas,0) : null,
        tempoSegundos:tempoCompleto ? (modelo.tempoTotalSegundos ?? placas.reduce((s,p)=>s+Number(p.tempoSegundos || 0),0)) : null,
        filamentos:[...soma.values()]};
}

function escalarConsumoConsultaRapida(base, unidades) {
    return Number((Number(base) * Number(unidades)).toFixed(3));
}

// Ler validity não dispara eventos invalid nem avisos enquanto a consulta é preenchida.
function consultaRapidaPronta(formulario) {
    return [...formulario.elements].every(campo => !campo.willValidate || campo.validity.valid);
}

function tokenConsultaRapida(formulario, doc=document) {
    return formulario.querySelector('input[name="_csrf"]')?.value || [...doc.querySelectorAll('meta[name="csrf-token"]')].at(-1)?.content || '';
}

function iniciarCalculoRapido() {
    const pagina=document.querySelector('[data-calculo-rapido]');
    if(!pagina || pagina.dataset.iniciado) return;
    pagina.dataset.iniciado='true';
    const buscar=s=>pagina.querySelector(s);
    const form=buscar('[data-rapido-form]'), lista=buscar('[data-rapido-materiais]');
    const arquivo=buscar('[data-rapido-arquivo]'), status=buscar('[data-rapido-importacao]');
    const erro=buscar('[data-rapido-erro]'), erroImportacao=buscar('[data-rapido-erro-importacao]');
    const resultado=buscar('[data-rapido-resultado]'), botao=buscar('[data-rapido-calcular]');
    const preview=buscar('[data-rapido-preview]'), imagem=buscar('[data-rapido-imagem]');
    const miniaturas=buscar('[data-rapido-miniaturas]'), importado=buscar('[data-rapido-importado]');
    const dialogo=buscar('[data-rapido-dialogo]');
    let placas=[], leitura=null, calculo=null, versao=0, minutosUnidade=null;
    const moeda=valor=>Number(valor || 0).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
    const numero=valor=>Number(valor || 0).toLocaleString('pt-BR',{maximumFractionDigits:3});
    const imagemValida=valor=>/^data:image\/(png|jpeg|webp);base64,[a-z0-9+/=]+$/i.test(valor || '');
    const headers=()=>({'X-CSRF-TOKEN':tokenConsultaRapida(form)});
    const modoEmbalagem=buscar('[data-rapido-modo-embalagem]');
    function atualizarEmbalagem(){
        if(!modoEmbalagem)return;
        for(const [modo,seletor] of [['cadastrada','[data-rapido-embalagem-cadastrada]'],['personalizada','[data-rapido-embalagem-personalizada]']]){
            const grupo=buscar(seletor);if(!grupo)continue;
            const ativo=modoEmbalagem.value===modo;grupo.hidden=!ativo;
            grupo.querySelectorAll('input,select').forEach(c=>{c.disabled=!ativo;c.required=ativo;});
        }
    }
    modoEmbalagem?.addEventListener('change',atualizarEmbalagem);atualizarEmbalagem();
    let precoTimer;
    const vivo=buscar('[data-rapido-preco-vivo]'), estadoVivo=buscar('[data-rapido-estado-vivo]');
    function invalidar() {
        versao++;resultado.hidden=true;erro.hidden=true;calculo?.abort();
        clearTimeout(precoTimer);
        if(vivo) vivo.textContent='Preencha os campos';
        if(estadoVivo) estadoVivo.textContent='Complete os campos para ver o preço.';
    }
    function agendarPreco() {
        clearTimeout(precoTimer);
        precoTimer=setTimeout(()=>{
            if(leitura || !consultaRapidaPronta(form) || !lista.children.length) return;
            form.requestSubmit();
        },650);
    }
    function informar(texto) {erro.textContent=texto;erro.hidden=!texto;}
    function informarImportacao(texto) {
        erroImportacao.textContent=texto;erroImportacao.hidden=!texto;
        if(texto) window.mostrarNotificacao?.(texto,'erro');
    }
    const unidades=()=>Math.max(1, Number(form.elements.unidades.value) || 1);
    function materialResumo(material, gramas=material.pesoGramas) {
        const span=document.createElement('span');span.className='rapido-material-resumo';
        const nome=document.createElement('span');nome.textContent=material.material || 'Filamento';
        const cor=document.createElement('span');cor.className='filamento-amostra';
        const codigo=material.cor?.slice(0,7);cor.hidden=!/^#[0-9a-f]{6}$/i.test(codigo || '');
        if(!cor.hidden) {cor.style.backgroundColor=codigo;cor.title=`Cor ${codigo}`;cor.setAttribute('aria-label',cor.title);}
        const peso=document.createElement('span');peso.textContent=`– ${numero(gramas)} g`;
        span.append(nome,cor,peso);return span;
    }
    function consumoLinha(linha) {
        const campo=linha.querySelector('input');
        const base=Number(linha.dataset.gramasUnidade || 0);
        linha.querySelector('[data-rapido-gramas-unidade]').textContent=`${numero(base)} g por arquivo completo`;
    }
    function atualizarTempoUnidade() {
        buscar('[data-rapido-tempo-unidade]').textContent=minutosUnidade!=null ? `${numero(minutosUnidade)} min por arquivo completo` : '';
    }
    form.elements.unidades.addEventListener('input',()=>{
        const n=Number(form.elements.unidades.value);if(!Number.isInteger(n) || n<1 || n>10000) return;
        for(const linha of lista.children) {
            if(linha.dataset.gramasUnidade) linha.querySelector('input').value=escalarConsumoConsultaRapida(linha.dataset.gramasUnidade,n);
            consumoLinha(linha);
        }
        if(minutosUnidade!=null) form.elements.tempoImpressaoMinutos.value=Math.ceil(minutosUnidade*n);
        pesoTotal();atualizarTempoUnidade();
    });
    form.elements.tempoImpressaoMinutos.addEventListener('input',()=>{
        minutosUnidade=form.elements.tempoImpressaoMinutos.value ? Number(form.elements.tempoImpressaoMinutos.value)/unidades() : null;
        atualizarTempoUnidade();
    });
    function pesoTotal() {
        const total=[...lista.querySelectorAll('input')].reduce((s,i)=>s+Number(i.value || 0),0);
        buscar('[data-rapido-peso-total]').textContent=`${numero(total)} g no lote · ${numero(total/unidades())} g por arquivo completo`;
    }
    function renumerar() {
        [...lista.children].forEach((linha,i)=>{
            const select=linha.querySelector('select'),gramas=linha.querySelector('input');
            select.name=`filamentos[${i}].filamentoId`;select.id=`rapido-filamento-${i}`;
            gramas.name=`filamentos[${i}].quantidadeFilamentoGramas`;gramas.id=`rapido-gramas-${i}`;
            linha.querySelector('[data-label-filamento]').htmlFor=select.id;
            linha.querySelector('[data-label-gramas]').htmlFor=gramas.id;
        });pesoTotal();
    }
    function adicionar(material) {
        if(lista.children.length>=100) {informar('Use até 100 filamentos.');return;}
        const linha=document.createElement('div');linha.className='rapido-material';
        const campo=document.createElement('div');campo.className='campo';
        const label=document.createElement('label');label.dataset.labelFilamento='';label.className='rapido-material-titulo';
        const titulo=document.createElement('span');titulo.textContent=material ? `Material ${material.id}: ${material.material || 'filamento'} (${material.cor || 'cor não informada'})` : 'Filamento adicional';
        const cor=document.createElement('span');cor.className='filamento-amostra';cor.setAttribute('aria-hidden','true');
        const codigo=material?.cor?.slice(0,7);cor.hidden=!/^#[0-9a-f]{6}$/i.test(codigo || '');
        if(!cor.hidden) cor.style.backgroundColor=codigo;
        label.append(titulo,cor);
        const select=buscar('[data-rapido-opcoes]').content.querySelector('select').cloneNode(true);select.required=true;
        select.addEventListener('change',()=>{
            if(material) return;
            const codigo=select.selectedOptions[0]?.dataset.cor;cor.hidden=!/^#[0-9a-f]{6}$/i.test(codigo || '');
            cor.style.backgroundColor=cor.hidden ? '' : codigo;
        });campo.append(label,select);
        const campoGramas=document.createElement('div');campoGramas.className='campo';
        const labelGramas=document.createElement('label');labelGramas.dataset.labelGramas='';labelGramas.textContent='Gramas totais';
        const gramas=document.createElement('input');gramas.type='number';gramas.min='0.001';gramas.max='1000000';gramas.step='0.001';gramas.required=true;
        const porUnidade=document.createElement('small');porUnidade.dataset.rapidoGramasUnidade='';porUnidade.className='rapido-material-consumo';
        if(Number(material?.pesoGramas)>0) {
            linha.dataset.gramasUnidade=material.pesoGramas;
            gramas.value=escalarConsumoConsultaRapida(material.pesoGramas,unidades());
        }
        gramas.addEventListener('input',()=>{linha.dataset.gramasUnidade=Number(gramas.value || 0)/unidades();consumoLinha(linha);pesoTotal();});
        campoGramas.append(labelGramas,gramas);
        const remover=document.createElement('button');remover.type='button';remover.className='botao botao-secundario';remover.textContent='Remover';
        remover.addEventListener('click',()=>{linha.remove();renumerar();invalidar();agendarPreco();});
        linha.append(campo,campoGramas,remover,porUnidade);lista.append(linha);consumoLinha(linha);renumerar();
    }
    function limparPreview() {importado.hidden=true;imagem.removeAttribute('src');miniaturas.replaceChildren();}
    imagem.addEventListener('error',()=>{imagem.hidden=true;buscar('[data-rapido-sem-imagem]').hidden=false;});
    function mostrarImagem(indice) {
        const placa=placas[indice];if(!placa) return;
        const tem=imagemValida(placa.imagemPreviewDataUri);imagem.hidden=!tem;
        buscar('[data-rapido-sem-imagem]').hidden=tem;
        if(tem) imagem.src=placa.imagemPreviewDataUri;else imagem.removeAttribute('src');
        buscar('[data-rapido-preview-legenda]').textContent=`Placa ${placa.numero} de ${placas.length} · ${placa.quantidadePecas ?? '?'} peça(s) · ${placa.tempoSegundos>0 ? numero(placa.tempoSegundos/60)+' min' : 'tempo não informado'} · ${placa.pesoGramas!=null ? numero(placa.pesoGramas)+' g' : 'peso não informado'}`;
        [...miniaturas.children].forEach((b,i)=>b.setAttribute('aria-pressed',String(i===indice)));
    }
    function preencherImportacao(modelo,nomeArquivo) {
        const resumo=resumirArquivoConsultaRapida(modelo);placas=resumo.placas;
        form.elements.nome.value=(modelo.nome?.trim() || nomeArquivo).slice(0,150);
        buscar('#rapidoUnidades').value=resumo.quantidadePecas ?? '';
        form.elements.unidades.value=1;
        form.elements.tempoImpressaoMinutos.value=resumo.tempoSegundos>0 ? Math.ceil(resumo.tempoSegundos/60) : '';
        minutosUnidade=form.elements.tempoImpressaoMinutos.value ? Number(form.elements.tempoImpressaoMinutos.value) : null;
        atualizarTempoUnidade();
        form.elements.numeroPlacas.value=resumo.numeroPlacas;
        lista.replaceChildren();resumo.filamentos.forEach(adicionar);if(!resumo.filamentos.length) adicionar();
        const normalizar=t=>String(t || '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').trim().toLowerCase();
        const impressoras=[...form.elements.impressoraId.options].filter(o=>o.value);
        const candidatas=impressoras.filter(o=>normalizar(o.dataset.modelo) && normalizar(o.dataset.modelo)===normalizar(modelo.modeloImpressora));
        if(candidatas.length===1) form.elements.impressoraId.value=candidatas[0].value;
        else if(impressoras.length===1) form.elements.impressoraId.value=impressoras[0].value;
        const metadados=buscar('[data-rapido-metadados]');metadados.replaceChildren();
        const extras=[['Impressora do perfil',modelo.modeloImpressora],['Altura de camada',modelo.alturaCamada ? modelo.alturaCamada+' mm' : ''],['Diâmetro do bico',modelo.diametroBico ? modelo.diametroBico+' mm' : ''],['Perfil de impressão',modelo.perfil],['Aplicativo',modelo.fatiador]];
        for(const [nome,valor] of extras) if(valor) {const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=nome;dd.textContent=valor;metadados.append(dt,dd);}
        const materiaisArquivo=buscar('[data-rapido-materiais-arquivo]');materiaisArquivo.replaceChildren();
        const tituloMateriais=document.createElement('h4');tituloMateriais.textContent='Materiais de um arquivo completo';materiaisArquivo.append(tituloMateriais);
        for(const material of resumo.filamentos) materiaisArquivo.append(materialResumo(material));
        buscar('[data-rapido-resumo]').textContent=`${resumo.numeroPlacas} placa(s) · ${resumo.quantidadePecas ?? 'Quantidade de peças não informada'}${resumo.quantidadePecas!=null ? ' peça(s)' : ''} · ${resumo.filamentos.length} material(is)`;
        miniaturas.replaceChildren(...placas.map((p,i)=>{
            const b=document.createElement('button');b.type='button';b.title=`Ver imagem da placa ${p.numero}`;b.setAttribute('aria-label',b.title);
            if(imagemValida(p.imagemPreviewDataUri)) {const img=document.createElement('img');img.src=p.imagemPreviewDataUri;img.alt='';b.append(img);}
            const n=document.createElement('span');n.textContent=p.numero;b.append(n);b.addEventListener('click',()=>mostrarImagem(i));return b;
        }));
        importado.hidden=false;mostrarImagem(0);
        const pendencias=[];
        if(resumo.quantidadePecas==null) pendencias.push('quantidade total de peças');
        if(!(resumo.tempoSegundos>0)) pendencias.push('tempo total de impressão');
        if(!resumo.filamentos.length || resumo.filamentos.some(f=>!(f.pesoGramas>0))) pendencias.push('gramas dos materiais');
        status.textContent=`Arquivo importado: todas as ${resumo.numeroPlacas} placa(s) consideradas. Escolha os rolos correspondentes.${pendencias.length ? ' O arquivo não trouxe dados completos para '+pendencias.join(', ')+'. Preencha os campos faltantes.' : ''}`;
    }
    function abrirDetalhes(tipo) {
        const conteudo=buscar('[data-rapido-dialogo-conteudo]');conteudo.replaceChildren();
        buscar('[data-rapido-dialogo-titulo]').textContent=tipo==='filamentos' ? 'Filamentos por placa' : tipo==='tempo' ? 'Tempo por placa' : 'Peças por placa';
        buscar('[data-rapido-dialogo-icone]').className=`bi ${tipo==='tempo' ? 'bi-stopwatch' : tipo==='filamentos' ? 'bi-palette' : 'bi-box'}`;
        if(placas.length) {const nota=document.createElement('p');nota.textContent='Dados do arquivo para um arquivo completo.';conteudo.append(nota);}
        if(!placas.length) {const p=document.createElement('p');p.textContent='Importe um arquivo para consultar as placas.';conteudo.append(p);}
        for(const placa of placas) {
            const bloco=document.createElement('section');bloco.className='rapido-placa-detalhe';
            const h=document.createElement('h3');h.textContent=`Placa ${placa.numero}`;
            const p=document.createElement('p');p.className='rapido-placa-linha';
            const dados=document.createElement('span');dados.textContent=`${placa.quantidadePecas ?? '?'} peça(s) · ${placa.tempoSegundos>0 ? numero(placa.tempoSegundos/60)+' min' : 'Tempo não informado'}`;
            p.append(dados);
            for(const f of placa.filamentos || []) {
                const separador=document.createElement('span');separador.textContent='·';separador.setAttribute('aria-hidden','true');p.append(separador,materialResumo(f));
            }
            if(!placa.filamentos?.length) {const peso=document.createElement('span');peso.textContent=`· ${placa.pesoGramas!=null ? numero(placa.pesoGramas)+' g (materiais não informados)' : 'Materiais não informados'}`;p.append(peso);}
            bloco.append(h,p);
            conteudo.append(bloco);
        }
        dialogo.showModal();
    }
    pagina.querySelectorAll('[data-rapido-detalhe]').forEach(b=>b.addEventListener('click',()=>abrirDetalhes(b.dataset.rapidoDetalhe)));
    buscar('[data-rapido-fechar-dialogo]').addEventListener('click',()=>dialogo.close());
    dialogo.addEventListener('click',e=>{if(e.target===dialogo) {const r=dialogo.getBoundingClientRect();if(e.clientX<r.left || e.clientX>r.right || e.clientY<r.top || e.clientY>r.bottom) dialogo.close();}});
    buscar('[data-rapido-adicionar]').addEventListener('click',()=>{adicionar();invalidar();});
    form.addEventListener('input',()=>{invalidar();agendarPreco();});form.addEventListener('change',()=>{invalidar();agendarPreco();});
    arquivo.addEventListener('change',async()=>{
        invalidar();leitura?.abort();leitura=null;limparPreview();informarImportacao('');placas=[];
        if(!arquivo.files[0]) {botao.disabled=false;status.textContent='';return;}
        form.elements.unidades.value=1;buscar('#rapidoUnidades').value='';minutosUnidade=null;lista.replaceChildren();adicionar();form.elements.tempoImpressaoMinutos.value='';atualizarTempoUnidade();form.elements.numeroPlacas.value=1;
        const nomeArquivo=arquivo.files[0].name.replace(/(?:\.gcode)?\.3mf$/i,'').slice(0,150);form.elements.nome.value=nomeArquivo;
        if(arquivo.files[0].size>100*1024*1024) {informarImportacao('O arquivo deve ter no máximo 100 MB.');botao.disabled=false;return;}
        const controle=new AbortController();leitura=controle;botao.disabled=true;status.textContent='Analisando todas as placas…';
        const dados=new FormData();dados.append('arquivo3mf',arquivo.files[0]);
        try {
            const resposta=await fetch(arquivo.dataset.url,{method:'POST',headers:headers(),body:dados,signal:controle.signal});
            const texto=await resposta.text();let modelo;
            try {modelo=JSON.parse(texto);} catch {
                throw new Error(resposta.status===413 ? 'O arquivo ultrapassa o limite de envio de 100 MB.' : 'Não foi possível ler a resposta. Recarregue a página para conferir a sessão e tente novamente.');
            }
            if(!resposta.ok) throw new Error(modelo.erro || 'Não foi possível analisar o arquivo.');
            if(leitura!==controle) return;
            preencherImportacao(modelo,nomeArquivo);
        } catch(e) {if(e.name!=='AbortError' && leitura===controle) {status.textContent='Importação não concluída.';informarImportacao(e.message);}}
        finally {if(leitura===controle) {leitura=null;botao.disabled=false;agendarPreco();}}
    });
    form.addEventListener('submit',async evento=>{
        if(evento.defaultPrevented) return;
        evento.preventDefault();if(leitura) return;
        if(!lista.children.length) {informar('Adicione pelo menos um filamento.');return;}
        invalidar();const atual=versao;const controle=new AbortController();calculo=controle;botao.disabled=true;if(estadoVivo) estadoVivo.textContent='Atualizando preço…';
        try {
            const resposta=await fetch(form.action,{method:'POST',headers:headers(),body:new FormData(form),signal:controle.signal});
            const dados=await resposta.json();if(!resposta.ok) throw new Error(dados.erro || 'Não foi possível calcular.');
            if(atual!==versao) return;
            pagina.querySelector('[data-rapido-custo-lote]').textContent=moeda(Number(dados.custoTotal)*dados.unidades);
            pagina.querySelector('[data-rapido-custo-unidade]').textContent=`${moeda(dados.custoTotal)} por arquivo completo`;
            const partes=buscar('#rapidoUnidades').value;
            buscar('[data-rapido-resultado-contexto]').textContent=`${dados.unidades} arquivo(s) completo(s)${partes ? ' · '+partes+' peças/partes por arquivo' : ''}.`;
            pagina.querySelector('[data-rapido-preco]').textContent=moeda(dados.precoSugerido);
            pagina.querySelector('[data-rapido-rateio]').textContent=moeda(dados.custoLoja);
            const detalhe=pagina.querySelector('[data-rapido-detalhamento]');detalhe.replaceChildren();
            const custos=[['Filamentos',dados.custoFilamento],['Energia',dados.custoEnergia],['Embalagem',dados.custoEmbalagem],['Purga',dados.custoPurga],['Mão de obra',dados.custoMaoObra],['Manutenção',dados.custoManutencao],['Depreciação',dados.custoDepreciacao],['Falhas estimadas',dados.custoFalha],...(dados.rateiosCustos || []).map(r=>[r.descricao,r.valorUnitario])];
            for(const [nome,valor] of custos) if(Number(valor)>0) {
                const linha=document.createElement('tr');
                for(const texto of [nome,moeda(valor),moeda(Number(valor)*dados.unidades)]) {const celula=document.createElement('td');celula.textContent=texto;linha.append(celula);}
                detalhe.append(linha);
            }
            resultado.hidden=false;
            if(vivo) vivo.textContent=moeda(dados.precoSugerido);
            if(estadoVivo) estadoVivo.textContent=`${dados.unidades} ${dados.unidades===1 ? 'arquivo completo' : 'arquivos completos'} · total sugerido ${moeda(Number(dados.precoSugerido)*dados.unidades)}`;
        } catch(e) {if(e.name!=='AbortError') informar(e instanceof SyntaxError ? 'A sessão pode ter expirado. Recarregue a página e tente novamente.' : e.message);}
        finally {if(calculo===controle) {calculo=null;botao.disabled=false;}}
    });
    function limpar() {
        leitura?.abort();leitura=null;invalidar();window.validacaoInline?.limparFormulario(form);pagina.querySelectorAll('.notificacao-flutuante.alerta-erro').forEach(aviso=>aviso.remove());form.reset();atualizarEmbalagem();minutosUnidade=null;atualizarTempoUnidade();arquivo.value='';placas=[];limparPreview();informarImportacao('');lista.replaceChildren();adicionar();status.textContent='';botao.disabled=false;
        if(dialogo.open) dialogo.close();
    }
    buscar('[data-rapido-limpar]').addEventListener('click',limpar);
    document.addEventListener('turbo:before-cache',()=>{limpar();delete pagina.dataset.iniciado;},{once:true});
    adicionar();
}
document.addEventListener('DOMContentLoaded',iniciarCalculoRapido);
document.addEventListener('turbo:load',iniciarCalculoRapido);

if(document.readyState === 'interactive' || document.readyState === 'complete') iniciarCalculoRapido();
