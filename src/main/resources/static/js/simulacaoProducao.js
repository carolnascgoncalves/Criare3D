(() => {
    const DURACAO_ANIMACAO_MS = 6500;
    const NOITE_COMECA_AS = 22;
    const NOITE_TERMINA_AS = 6;
    const OBJETO = { base: 135, topo: 44 };
    const DIAS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];
    const CAMPOS = ['formulario', 'calculo', 'unidades', 'por-impressao', 'inicio', 'usar-agora', 'intervalo', 'folga', 'erro', 'relogio-data', 'relogio-hora', 'impressao-atual', 'pecas-prontas', 'camada', 'corte', 'portal', 'bico', 'linha', 'eixo', 'resultado', 'pronto-em', 'detalhe', 'referencia', 'plano', 'plano-lista', 'pular'];
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
        for (let d = new Date(p.inicio.getFullYear(), p.inicio.getMonth(), p.inicio.getDate() - 1, NOITE_COMECA_AS); d < fim; d = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1)) {
            const termina = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1, NOITE_TERMINA_AS);
            const de = Math.max(0, (d - p.inicio) / 60000), ate = Math.min(p.eixo, (termina - p.inicio) / 60000);
            if (ate > de) criar('sim-noite', { left: pct(de), width: pct(ate - de) });
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
        if (entradas.erro) { ui.erro.textContent = entradas.erro; ui.erro.hidden = false; entradas.campo.setAttribute('aria-invalid', 'true'); entradas.campo.focus(); return; }
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
        const id = ui.calculo.value;
        const tentativa = ++estado.tentativaBusca;
        estado.base = null; ui.referencia.hidden = true; ui.plano.hidden = true; ui.resultado.hidden = true;
        if (!id) return;
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
            const referencia = document.createElement('p');
            referencia.textContent = `${dados.nomePeca} · ${dados.nomeImpressora || 'Impressora não informada'} · ${dados.tempoPorUnidadeMinutos || 0} min por unidade${dados.aquecimentoMinutos > 0 ? ` · ${dados.aquecimentoMinutos} min de aquecimento por impressão` : ''}`;
            ui.referencia.replaceChildren(referencia); ui.referencia.hidden = false; limparErro(ui);
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
})();
