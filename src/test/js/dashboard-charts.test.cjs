const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const codigo = fs.readFileSync('src/main/resources/static/js/dashboardCharts.js', 'utf8');

function preparar(temPainel = true) {
    const scripts = [];
    const eventos = new Map();
    const raiz = {dataset: {chartSrc: '/cost/vendor/chart-versao123.umd.min.js'}, textContent: '{}'};
    let painel = temPainel ? raiz : null;
    let leiturasDeEstilo = 0;
    const document = {
        readyState: 'complete',
        documentElement: {},
        addEventListener(nome, callback) { eventos.set(nome, callback); },
        getElementById(id) { return id === 'dashboard-chart-data' ? painel : null; },
        createElement() { return {}; },
        head: {appendChild(script) { scripts.push(script); }}
    };
    const window = {addEventListener() {}};
    vm.runInNewContext(codigo, {
        document, window, console: {error() {}},
        requestAnimationFrame: callback => callback(),
        getComputedStyle() { leiturasDeEstilo++; return {getPropertyValue() { return ''; }}; }
    });
    return {scripts, eventos, window, sair() { painel = null; }, estilos() { return leiturasDeEstilo; }};
}

test('dashboard carrega a URL versionada com contexto mesmo depois do turbo:load', async () => {
    const ambiente = preparar();
    assert.equal(ambiente.scripts.length, 1);
    assert.equal(ambiente.scripts[0].src, '/cost/vendor/chart-versao123.umd.min.js');
    ambiente.eventos.get('turbo:load')();
    assert.equal(ambiente.scripts.length, 1);
    ambiente.scripts[0].onload();
    await new Promise(setImmediate);
    assert.ok(ambiente.estilos() > 0);
});

test('falha ao baixar os graficos permite nova tentativa', async () => {
    const ambiente = preparar();
    ambiente.scripts[0].onerror(new Error('Falha de rede'));
    await new Promise(setImmediate);
    assert.equal(ambiente.window.criareCarregamentoChart, null);
    ambiente.eventos.get('turbo:load')();
    assert.equal(ambiente.scripts.length, 2);
});

test('sair da dashboard enquanto baixa a biblioteca nao desenha dados antigos', async () => {
    const ambiente = preparar();
    ambiente.sair();
    ambiente.scripts[0].onload();
    await new Promise(setImmediate);
    assert.equal(ambiente.estilos(), 0);
    assert.equal(preparar(false).scripts.length, 0);
});
