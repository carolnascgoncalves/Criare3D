const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const codigo = fs.readFileSync('src/main/resources/static/js/rascunhoPersistente.js', 'utf8');

function preparar(fetch) {
    const window = {}, status = {textContent: ''};
    vm.runInNewContext(codigo, {window, fetch, encodeURIComponent, setTimeout() {return 1;}, clearTimeout() {},
        document: {getElementById: () => ({value: 'calculo-1'}), querySelector: () => status,
            querySelectorAll: () => [{content: 'csrf-atual'}]}});
    return {fila: window.filaRascunhoCalculo, status};
}
const dados = valor => ({campos: {unidades: valor}, filamentos: [], etapa: 2, atualizadoEm: Date.now()});

test('rascunho serializa envios para a ultima edicao nao ser sobrescrita por uma resposta antiga', async () => {
    const envios = [], concluir = [];
    const {fila} = preparar((url, options) => new Promise(resolve => {envios.push({url, options});concluir.push(resolve);}));
    fila.agendar('calculo-1', dados('2'));const primeiro = fila.sincronizar();
    fila.agendar('calculo-1', dados('3'));const segundo = fila.sincronizar();
    await new Promise(setImmediate);
    assert.equal(envios.length, 1);
    concluir[0]({ok: true});await primeiro;await new Promise(setImmediate);
    assert.equal(envios.length, 2);
    assert.equal(JSON.parse(envios[1].options.body).campos.unidades, '3');
    assert.equal(envios[1].options.headers['X-CSRF-TOKEN'], 'csrf-atual');
    assert.match(envios[1].url, /rascunhoId=calculo-1/);
    concluir[1]({ok: true});await segundo;
});

test('falha preserva trabalho para tentar novamente e informa que so a copia local esta disponivel', async () => {
    let chamadas = 0;
    const {fila, status} = preparar(async () => ({ok: ++chamadas > 1}));
    fila.agendar('calculo-1', dados('7'));
    await assert.rejects(fila.sincronizar());
    assert.match(status.textContent, /cópia permanece/);
    await fila.sincronizar();
    assert.equal(chamadas, 2);assert.equal(status.textContent, 'Rascunho salvo.');
});

test('rascunhos diferentes nao compartilham deduplicacao e refazer descarta somente o pendente indicado', async () => {
    const envios = [];
    const {fila} = preparar(async url => {envios.push(url);return {ok: true};});
    fila.agendar('calculo-1', dados('5'));await fila.sincronizar();
    fila.agendar('calculo-1', dados('5'));await fila.sincronizar();
    assert.equal(envios.length, 1);
    fila.agendar('calculo-2', dados('5'));await fila.sincronizar();
    assert.equal(envios.length, 2);
    fila.agendar('calculo-1', dados('8'));fila.descartar('calculo-1');await fila.sincronizar();
    assert.equal(envios.length, 2);
});
