(() => {
    const DURACAO_ANIMACAO_MS = 6500;
    const NOITE_COMECA_AS = 18;
    const NOITE_TERMINA_AS = 6;
    const OBJETO = { base: 135, topo: 44 };
    const DIAS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];
    const CAMPOS = ['formulario', 'calculo', 'abrir-seletor', 'seletor-painel', 'buscar-calculo', 'filtrar-data', 'limpar-selecao', 'lista-calculos', 'seletor-vazio', 'unidades', 'por-impressao', 'inicio', 'usar-agora', 'intervalo', 'folga', 'erro', 'relogio-data', 'relogio-hora', 'relogio-duracao', 'impressao-atual', 'pecas-prontas', 'camada', 'corte', 'portal', 'bico', 'linha', 'eixo', 'resultado', 'pronto-em', 'detalhe', 'referencia', 'plano', 'plano-lista', 'pular'];
    const pad = n => String(n).padStart(2, '0');
    const dataCurta = d => `${DIAS[d.getDay()]}, ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
    const hora = d => `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    const dataCompleta = d => `${dataCurta(d)} às ${hora(d)}`;
    const emMinutos = (base, minutos) => new Date(base.getTime() + minutos * 60000);
    const valorParaInput = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${hora(d)}`;
    const formatarDuracao = minutos => {
        const total = Math.round(minutos), h = Math.floor(total / 60);
        return h > 0 ? `${h}h ${pad(total % 60)}min` : `${total}min`;
    };
    const formatarGramas = valor => `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(valor)} g`;
    const normalizarTexto = texto => (texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');

    function dataDaOpcao(opcao) {
        const valor = opcao.dataset.data || '';
        const data = new Date(valor);
        return Number.isNaN(data.getTime()) ? null : data;
    }

    function rotuloRelativo(data) {
        if (!data) return 'Data não informada';
        const hoje = new Date();
        const ontem = new Date(hoje); ontem.setDate(hoje.getDate() - 1);
        const mesmoDia = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
        if (mesmoDia(data, hoje)) return `Hoje, ${hora(data)}`;
        if (mesmoDia(data, ontem)) return `Ontem, ${hora(data)}`;
        return `${pad(data.getDate())}/${pad(data.getMonth() + 1)}, ${hora(data)}`;
    }

    function duracaoDoCalculo(opcao) {
        const unidades = Number(opcao.dataset.unidades || 0);
        const tempo = Number(opcao.dataset.tempo || 0);
        const capacidade = Math.max(1, Number(opcao.dataset.porImpressao || 1));
        const aquecimento = Math.max(0, Number(opcao.dataset.aquecimento || 0));
        return unidades * tempo + Math.ceil(unidades / capacidade) * aquecimento;
    }

    function renderizarSeletor(ui) {
        const termo = normalizarTexto(ui.buscarCalculo.value);
        const dataFiltro = ui.filtrarData.value;
        const opcoes = [...ui.calculo.options].filter(opcao => opcao.value);
        const visiveis = opcoes.filter(opcao => {
            const data = dataDaOpcao(opcao);
            const nomeCorresponde = normalizarTexto(opcao.dataset.nome).includes(termo);
            const dataCorresponde = !dataFiltro || (data && `${data.getFullYear()}-${pad(data.getMonth() + 1)}-${pad(data.getDate())}` === dataFiltro);
            return nomeCorresponde && dataCorresponde;
        });
        const grupos = new Map([['Hoje', []], ['Ontem', []], ['Anteriores', []]]);
        visiveis.forEach(opcao => {
            const data = dataDaOpcao(opcao), hoje = new Date(), ontem = new Date(hoje);
            ontem.setDate(hoje.getDate() - 1);
            const mesmoDia = (a, b) => a && a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
            const grupo = mesmoDia(data, hoje) ? 'Hoje' : mesmoDia(data, ontem) ? 'Ontem' : 'Anteriores';
            grupos.get(grupo).push(opcao);
        });
        const fragmento = document.createDocumentFragment();
        grupos.forEach((itens, titulo) => {
            if (!itens.length) return;
            const cabecalho = document.createElement('h3'); cabecalho.className = 'sim-seletor-grupo'; cabecalho.textContent = titulo;
            fragmento.appendChild(cabecalho);
            itens.forEach(opcao => {
                const item = document.createElement('button'); item.type = 'button'; item.className = 'sim-seletor-item'; item.setAttribute('role', 'option');
                item.setAttribute('aria-selected', String(opcao.value === ui.calculo.value)); item.dataset.calculoId = opcao.value;
                const estado = estadoDaOpcao(opcao);
                item.classList.add(`sim-status-${estado}`);
                if (opcao.dataset.temFoto === 'true') {
                    const foto = document.createElement('img'); foto.className = 'sim-seletor-foto'; foto.alt = ''; foto.loading = 'lazy';
                    foto.src = `${ui.painel.dataset.urlHistorico}/${encodeURIComponent(opcao.value)}/foto?miniatura=true`;
                    foto.addEventListener('error', () => foto.replaceWith(criarIconeSeletor(estado)), { once: true });
                    item.appendChild(foto);
                } else item.appendChild(criarIconeSeletor(estado));
                const textos = document.createElement('span'); textos.className = 'sim-seletor-textos';
                const nome = document.createElement('strong'); nome.className = 'sim-seletor-nome'; nome.textContent = opcao.dataset.nome || 'Peça sem nome';
                const resumo = document.createElement('span'); resumo.className = 'sim-seletor-resumo';
                const unidades = Number(opcao.dataset.unidades || 0);
                resumo.textContent = `${unidades} ${unidades === 1 ? 'unidade' : 'unidades'} · ${formatarDuracao(duracaoDoCalculo(opcao))}`;
                textos.append(nome, resumo); item.appendChild(textos);
                const data = document.createElement('time'); data.className = 'sim-seletor-horario';
                const dataCalculo = dataDaOpcao(opcao); data.textContent = rotuloRelativo(dataCalculo);
                if (dataCalculo) data.dateTime = dataCalculo.toISOString();
                item.appendChild(data);
                item.addEventListener('click', () => {
                    ui.calculo.value = opcao.value;
                    atualizarBotaoSeletor(ui, opcao);
                    fecharSeletor(ui);
                    ui.calculo.dispatchEvent(new Event('change', { bubbles: true }));
                });
                fragmento.appendChild(item);
            });
        });
        ui.listaCalculos.replaceChildren(fragmento);
        ui.seletorVazio.hidden = visiveis.length > 0;
        ui.limparSelecao.hidden = !ui.calculo.value;
    }

    function estadoDaOpcao(opcao) {
        return opcao.dataset.vendido === 'true' ? 'vendido' : opcao.dataset.produzido === 'true' ? 'estoque' : 'calculo';
    }

    function criarIconeSeletor(estado = 'calculo') {
        const icone = document.createElement('span'); icone.className = `sim-seletor-sem-foto sim-status-${estado}`;
        const imagem = document.createElement('i'); imagem.className = 'bi bi-image'; imagem.setAttribute('aria-hidden', 'true');
        icone.appendChild(imagem); return icone;
    }

    function atualizarBotaoSeletor(ui, opcao) {
        if (!opcao || !opcao.value) {
            ui.abrirSeletor.setAttribute('aria-label', 'Escolher cálculo de referência');
            ui.abrirSeletor.replaceChildren();
            const placeholder = document.createElement('span'); placeholder.className = 'sim-seletor-placeholder'; placeholder.textContent = 'Escolha um cálculo do histórico';
            const icone = document.createElement('i'); icone.className = 'bi bi-chevron-down'; icone.setAttribute('aria-hidden', 'true');
            ui.abrirSeletor.append(placeholder, icone); return;
        }
        const fragmento = document.createDocumentFragment();
        ui.abrirSeletor.classList.remove('sim-status-calculo', 'sim-status-estoque', 'sim-status-vendido');
        ui.abrirSeletor.classList.add(`sim-status-${estadoDaOpcao(opcao)}`);
        if (opcao.dataset.temFoto === 'true') {
            const foto = document.createElement('img'); foto.className = 'sim-seletor-foto sim-seletor-foto-selecionada'; foto.alt = ''; foto.src = `${ui.painel.dataset.urlHistorico}/${encodeURIComponent(opcao.value)}/foto?miniatura=true`;
            foto.addEventListener('error', () => foto.replaceWith(criarIconeSeletor(estadoDaOpcao(opcao))), { once: true }); fragmento.appendChild(foto);
        } else fragmento.appendChild(criarIconeSeletor(estadoDaOpcao(opcao)));
        const textos = document.createElement('span'); textos.className = 'sim-seletor-textos';
        const nome = document.createElement('strong'); nome.className = 'sim-seletor-nome'; nome.textContent = opcao.dataset.nome || 'Peça sem nome';
        const unidades = Number(opcao.dataset.unidades || 0), resumo = document.createElement('span'); resumo.className = 'sim-seletor-resumo';
        resumo.textContent = `${unidades} ${unidades === 1 ? 'unidade' : 'unidades'} · ${formatarDuracao(duracaoDoCalculo(opcao))} · ${rotuloRelativo(dataDaOpcao(opcao))}`;
        ui.abrirSeletor.setAttribute('aria-label', `Cálculo selecionado: ${opcao.dataset.nome}, ${resumo.textContent}. Abrir lista`);
        textos.append(nome, resumo); fragmento.appendChild(textos);
        const seta = document.createElement('i'); seta.className = 'bi bi-chevron-down'; seta.setAttribute('aria-hidden', 'true'); fragmento.appendChild(seta);
        ui.abrirSeletor.replaceChildren(fragmento);
    }

    function fecharSeletor(ui) {
        ui.seletorPainel.hidden = true; ui.abrirSeletor.setAttribute('aria-expanded', 'false');
    }

    function lerEntradas(ui, estado) {
        const base = estado.base;
        const unidades = Number.parseInt(ui.unidades.value, 10), porImpressao = Number.parseInt(ui.porImpressao.value, 10);
        const intervalo = Math.max(0, Number.parseInt(ui.intervalo.value, 10) || 0), folga = Number.parseInt(ui.folga.value, 10);
        const inicio = new Date(ui.inicio.value);
        if (!base) return { erro: 'Escolha um cálculo de referência do histórico.', campo: ui.calculo };
        if (!(base.tempoPorUnidadeMinutos > 0)) return { erro: 'Este cálculo não tem um tempo de impressão válido para simular.', campo: ui.calculo };
        if (!(Number.isInteger(unidades) && unidades >= 1) || unidades > 500) return { erro: 'Informe de 1 a 500 unidades.', campo: ui.unidades };
        if (!(Number.isInteger(porImpressao) && porImpressao >= 1) || porImpressao > unidades) return { erro: 'Peças por impressão deve ser de 1 até a quantidade de unidades.', campo: ui.porImpressao };
        if (!(folga >= 0 && folga <= 100)) return { erro: 'A folga deve ficar entre 0% e 100%.', campo: ui.folga };
        if (Number.isNaN(inicio.getTime())) return { erro: 'Informe o horário de início.', campo: ui.inicio };
        return { ...base, unidades, porImpressao, intervalo, folga, inicio };
    }

    function planejar(e) {
        const impressoes = Math.ceil(e.unidades / e.porImpressao), execucoes = [];
        let restante = e.unidades, deslocamento = 0;
        for (let i = 0; i < impressoes; i++) {
            const unidadesNaImpressao = Math.min(e.porImpressao, restante);
            const duracao = unidadesNaImpressao * e.tempoPorUnidadeMinutos + e.aquecimentoMinutos;
            const gramas = e.filamentos.map(item => ({ ...item, totalGramas: Number(item.gramasPorUnidade || 0) * unidadesNaImpressao }));
            execucoes.push({ indice: i, unidades: unidadesNaImpressao, inicio: deslocamento, duracao, fim: deslocamento + duracao, filamentos: gramas });
            deslocamento += duracao + (i < impressoes - 1 ? e.intervalo : 0);
            restante -= unidadesNaImpressao;
        }
        const total = deslocamento, folgaMin = Math.round(total * e.folga / 100);
        return { ...e, impressoes, execucoes, total, folgaMin, eixo: total + folgaMin };
    }

    function desenharLinha(ui, p) {
        const fim = emMinutos(p.inicio, p.eixo), pct = minutos => `${(minutos / p.eixo) * 100}%`;
        const fragmento = document.createDocumentFragment();
        const criar = (classe, estilo) => { const el = document.createElement('div'); el.className = classe; Object.assign(el.style, estilo); fragmento.appendChild(el); return el; };
        for (let inicio = new Date(p.inicio); inicio < fim;) {
            const noite = inicio.getHours() < NOITE_TERMINA_AS || inicio.getHours() >= NOITE_COMECA_AS;
            const limite = new Date(inicio);
            if (inicio.getHours() < NOITE_TERMINA_AS) limite.setHours(NOITE_TERMINA_AS, 0, 0, 0);
            else if (inicio.getHours() < NOITE_COMECA_AS) limite.setHours(NOITE_COMECA_AS, 0, 0, 0);
            else { limite.setDate(limite.getDate() + 1); limite.setHours(NOITE_TERMINA_AS, 0, 0, 0); }
            const de = (inicio - p.inicio) / 60000, ate = Math.min(p.eixo, (limite - p.inicio) / 60000);
            criar(noite ? 'sim-noite' : 'sim-periodo-dia', { left: pct(de), width: pct(ate - de) });
            inicio = limite;
        }
        if (p.folgaMin > 0) criar('sim-folga', { left: pct(p.total), width: pct(p.folgaMin) });
        const meiasNoites = [];
        for (let d = new Date(p.inicio.getFullYear(), p.inicio.getMonth(), p.inicio.getDate() + 1); d < fim; d = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1)) {
            const minutos = (d - p.inicio) / 60000, posicao = minutos / p.eixo * 100;
            if (posicao > 8 && posicao < 92) meiasNoites.push({ d, minutos });
        }
        const salto = Math.max(1, Math.ceil(meiasNoites.length / 6)), rotulos = [{ minutos: 0, texto: hora(p.inicio), inicio: true }];
        meiasNoites.forEach(({ d, minutos }, i) => { if (i % salto === 0) { criar('sim-dia', { left: pct(minutos) }); rotulos.push({ minutos, texto: `${pad(d.getDate())}/${pad(d.getMonth() + 1)}` }); } });
        const preenchimentos = [];
        p.execucoes.forEach(execucao => {
            const bloco = criar('sim-bloco', { left: pct(execucao.inicio), width: pct(execucao.duracao) });
            const fill = document.createElement('div'); fill.className = 'sim-bloco-fill'; bloco.appendChild(fill); preenchimentos.push(fill);
        });
        const agora = criar('sim-agora', { left: '0%' });
        ui.linha.replaceChildren(fragmento);
        ui.eixo.replaceChildren(...rotulos.map(r => { const el = document.createElement('span'); el.textContent = r.texto; el.style.left = pct(r.minutos); if (r.inicio) el.classList.add('sim-eixo-inicio'); return el; }));
        return { preenchimentos, agora };
    }

    function desenharPlano(ui, p) {
        const fragmento = document.createDocumentFragment();
        p.execucoes.forEach(execucao => {
            const linha = document.createElement('div'); linha.className = 'sim-plano-linha';
            const titulo = document.createElement('strong'); titulo.textContent = `Impressão ${execucao.indice + 1}`;
            const resumo = document.createElement('span'); resumo.textContent = `${execucao.unidades} ${execucao.unidades === 1 ? 'unidade' : 'unidades'} · ${formatarDuracao(execucao.duracao)}${p.aquecimentoMinutos > 0 ? ` (inclui ${p.aquecimentoMinutos} min de aquecimento)` : ''}`;
            const materiais = document.createElement('span'); materiais.className = 'sim-plano-materiais';
            const partes = execucao.filamentos.filter(item => item.totalGramas > 0).map(item => `${item.nome}: ${formatarGramas(item.totalGramas)}`);
            materiais.textContent = partes.length ? partes.join(' · ') : 'Filamento não informado no cálculo';
            linha.append(titulo, resumo, materiais); fragmento.appendChild(linha);
        });
        ui.planoLista.replaceChildren(fragmento);
        ui.plano.hidden = false;
    }

    function desenharCena(ui, progresso, ts) {
        const y = OBJETO.base - progresso * (OBJETO.base - OBJETO.topo), topo = y - 16;
        const x = ts === null || progresso >= 1 ? 100 : 100 + Math.sin(ts / 150) * 24;
        ui.corte.setAttribute('y', y); ui.portal.setAttribute('y1', topo); ui.portal.setAttribute('y2', topo); ui.bico.setAttribute('transform', `translate(${x} ${topo})`);
    }

    function quadro(ui, p, vis, fracao, ts) {
        const decorrido = fracao * p.total;
        let atual = p.execucoes.find(execucao => decorrido < execucao.fim) || p.execucoes[p.execucoes.length - 1];
        const concluidas = p.execucoes.filter(execucao => decorrido >= execucao.fim);
        const progresso = fracao >= 1 ? 1 : Math.max(0, Math.min(1, (decorrido - atual.inicio) / atual.duracao));
        const prontas = concluidas.reduce((soma, item) => soma + item.unidades, 0), agora = emMinutos(p.inicio, decorrido);
        ui.relogioData.textContent = dataCurta(agora); ui.relogioHora.textContent = hora(agora);
        ui.relogioDuracao.textContent = `${formatarDuracao(decorrido)} de ${formatarDuracao(p.total)}`;
        ui.impressaoAtual.textContent = `${Math.min(concluidas.length + 1, p.impressoes)} de ${p.impressoes}`;
        ui.pecasProntas.textContent = `${fracao >= 1 ? p.unidades : prontas} de ${p.unidades}`;
        ui.camada.textContent = `${Math.round((fracao >= 1 ? 1 : progresso) * 100)}%`;
        vis.preenchimentos.forEach((el, i) => {
            const execucao = p.execucoes[i];
            const parte = decorrido >= execucao.fim ? 1 : decorrido <= execucao.inicio ? 0 : Math.min(1, (decorrido - execucao.inicio) / execucao.duracao);
            el.style.transform = `scaleX(${parte})`;
        });
        vis.agora.style.left = `${decorrido / p.eixo * 100}%`;
        desenharCena(ui, progresso, fracao < 1 && decorrido >= atual.inicio && decorrido < atual.fim ? ts : null);
    }

    function finalizar(ui, p, estado) {
        estado.executando = false; ui.pular.hidden = true;
        const previsto = emMinutos(p.inicio, p.total), limite = emMinutos(p.inicio, p.eixo);
        ui.prontoEm.textContent = dataCompleta(previsto);
        ui.detalhe.textContent = `${p.unidades} unidades em ${p.impressoes} ${p.impressoes === 1 ? 'impressão' : 'impressões'} · ${formatarDuracao(p.total)} de produção${p.intervalo > 0 && p.impressoes > 1 ? ` · ${formatarDuracao(p.intervalo * (p.impressoes - 1))} em intervalos` : ''}`
            + (p.folgaMin > 0 ? ` · com ${p.folga}% de folga, até ${dataCompleta(limite)}` : '');
        ui.resultado.hidden = false;
    }

    function limparErro(ui) { ui.erro.hidden = true; ui.erro.textContent = ''; ui.painel.querySelectorAll('[aria-invalid]').forEach(el => el.removeAttribute('aria-invalid')); }
    function executar(ui, estado) {
        cancelAnimationFrame(estado.raf);
        const entradas = lerEntradas(ui, estado);
        if (entradas.erro) { ui.erro.hidden = true; window.mostrarNotificacao?.(entradas.erro, 'erro'); window.validacaoInline?.destacar(entradas.campo); (entradas.campo === ui.calculo ? ui.abrirSeletor : entradas.campo).focus(); return; }
        limparErro(ui);
        const p = planejar(entradas), vis = desenharLinha(ui, p); desenharPlano(ui, p);
        ui.resultado.hidden = true;
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { quadro(ui, p, vis, 1, null); return finalizar(ui, p, estado); }
        estado.executando = true; estado.plano = p; estado.vis = vis; ui.pular.hidden = false;
        const inicioAnimacao = performance.now(), passo = agora => {
            if (!ui.painel.isConnected || !estado.executando) return;
            const fracao = Math.min(1, (agora - inicioAnimacao) / DURACAO_ANIMACAO_MS);
            quadro(ui, p, vis, fracao, agora);
            if (fracao < 1) estado.raf = requestAnimationFrame(passo); else finalizar(ui, p, estado);
        };
        estado.raf = requestAnimationFrame(passo);
    }

    async function carregarCalculo(ui, estado) {
        cancelAnimationFrame(estado.raf);
        estado.executando = false;
        ui.pular.hidden = true;
        const id = ui.calculo.value;
        const tentativa = ++estado.tentativaBusca;
        estado.base = null; ui.referencia.hidden = true; ui.plano.hidden = true; ui.resultado.hidden = true;
        if (!id) { atualizarBotaoSeletor(ui, null); renderizarSeletor(ui); return; }
        ui.calculo.disabled = true;
        try {
            const resposta = await fetch(`${ui.painel.dataset.urlCalculo}/${encodeURIComponent(id)}`, { headers: { Accept: 'application/json' } });
            if (!resposta.ok) throw new Error('Não foi possível carregar os dados desse cálculo. Escolha outro item do histórico.');
            const dados = await resposta.json();
            if (tentativa !== estado.tentativaBusca) return;
            estado.base = {
                tempoPorUnidadeMinutos: Number(dados.tempoPorUnidadeMinutos || 0),
                aquecimentoMinutos: Number(dados.aquecimentoMinutos || 0),
                filamentos: Array.isArray(dados.filamentos) ? dados.filamentos : []
            };
            ui.unidades.value = dados.unidades || 1;
            ui.porImpressao.value = Math.min(dados.pecasPorImpressao || 1, Number(ui.unidades.value));
            const cartao = document.createElement('div'); cartao.className = 'sim-referencia-cartao';
            if (dados.temFoto) {
                const foto = document.createElement('img');
                foto.src = `${ui.painel.dataset.urlHistorico}/${encodeURIComponent(dados.id)}/foto?miniatura=true`;
                foto.alt = '';
                foto.loading = 'lazy';
                foto.className = 'sim-referencia-foto';
                foto.addEventListener('error', () => foto.remove(), { once: true });
                cartao.appendChild(foto);
            }
            const conteudo = document.createElement('div'); conteudo.className = 'sim-referencia-conteudo';
            const nome = document.createElement('strong'); nome.textContent = dados.nomePeca;
            const data = new Date(dados.dataCalculo);
            const registro = document.createElement('span');
            registro.textContent = `${Number(dados.unidades || 0)} ${Number(dados.unidades) === 1 ? 'unidade' : 'unidades'} · ${Number.isNaN(data.getTime()) ? '' : `${pad(data.getDate())}/${pad(data.getMonth() + 1)}/${data.getFullYear()} às ${hora(data)}`}`;
            const impressora = document.createElement('span');
            impressora.textContent = `${dados.nomeImpressora || 'Impressora não informada'} · ${dados.tempoPorUnidadeMinutos || 0} min por unidade${dados.aquecimentoMinutos > 0 ? ` · ${dados.aquecimentoMinutos} min de aquecimento por impressão` : ''}`;
            conteudo.append(nome, registro, impressora); cartao.appendChild(conteudo);
            ui.referencia.replaceChildren(cartao); ui.referencia.hidden = false; limparErro(ui);
        } catch (erro) {
            if (tentativa !== estado.tentativaBusca) return;
            ui.erro.textContent = erro.message || 'Não foi possível carregar esse cálculo.'; ui.erro.hidden = false;
        } finally { ui.calculo.disabled = false; }
    }

    function inicializarSimulacao() {
        const painel = document.querySelector('[data-simulacao]');
        if (!painel || painel.dataset.simulacaoInicializada === 'true') return;
        painel.dataset.simulacaoInicializada = 'true';
        const ui = { painel };
        CAMPOS.forEach(nome => { const chave = nome.replace(/-([a-z])/g, (_, letra) => letra.toUpperCase()); ui[chave] = painel.querySelector(`[data-sim="${nome}"]`); });
        if (!ui.formulario) return;
        const estado = { raf: 0, base: null, executando: false, tentativaBusca: 0 };
        ui.inicio.value = valorParaInput(new Date());
        renderizarSeletor(ui);
        const abrir = () => {
            ui.seletorPainel.hidden = !ui.seletorPainel.hidden;
            ui.abrirSeletor.setAttribute('aria-expanded', String(!ui.seletorPainel.hidden));
            if (!ui.seletorPainel.hidden) { renderizarSeletor(ui); ui.buscarCalculo.focus(); }
        };
        ui.abrirSeletor.addEventListener('click', abrir);
        ui.buscarCalculo.addEventListener('input', () => renderizarSeletor(ui));
        ui.filtrarData.addEventListener('change', () => renderizarSeletor(ui));
        ui.limparSelecao.addEventListener('click', () => {
            ui.calculo.value = ''; atualizarBotaoSeletor(ui, null); ui.referencia.hidden = true; ui.base = null;
            fecharSeletor(ui); renderizarSeletor(ui);
        });
        ui.seletorPainel.addEventListener('click', evento => evento.stopPropagation());
        document.addEventListener('click', evento => {
            if (!ui.seletorPainel.hidden && !evento.target.closest('.sim-seletor-abrir, .sim-seletor-painel')) fecharSeletor(ui);
        });
        ui.calculo.addEventListener('change', () => carregarCalculo(ui, estado));
        ui.formulario.addEventListener('submit', evento => { evento.preventDefault(); executar(ui, estado); });
        ui.usarAgora.addEventListener('click', () => { ui.inicio.value = valorParaInput(new Date()); });
        ui.pular.addEventListener('click', () => {
            if (!estado.executando) return;
            cancelAnimationFrame(estado.raf); estado.executando = false;
            quadro(ui, estado.plano, estado.vis, 1, null); finalizar(ui, estado.plano, estado);
        });
        ui.formulario.addEventListener('input', () => limparErro(ui));
        desenharCena(ui, 0, null);
    }
    document.addEventListener('DOMContentLoaded', inicializarSimulacao);
    document.addEventListener('turbo:load', inicializarSimulacao);
    document.addEventListener('turbo:render', inicializarSimulacao);
    if (document.readyState !== 'loading') inicializarSimulacao();
})();
