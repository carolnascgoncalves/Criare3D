const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const codigo = fs.readFileSync('src/main/resources/static/js/modelosImpressora.js', 'utf8');

test('cadastro completo e painel compartilham a consulta de modelos sem baixar duas vezes', async () => {
    const window = {};let chamadas = 0;
    const modelos = [{nome: 'Modelo de referência', potenciaMediaPLA: 90}];
    vm.runInNewContext(codigo, {window, document: {addEventListener() {}},
        fetch: async url => { chamadas++;assert.equal(url, '/catalogo-opcoes/modelos-impressora');
            return {ok: true, json: async () => modelos}; }
    });
    const primeiro = window.carregarModelosImpressora();
    const segundo = window.carregarModelosImpressora();
    assert.equal(primeiro, segundo);assert.equal(await primeiro, modelos);assert.equal(chamadas, 1);
});

test('falha ao carregar referencias permite tentar novamente sem fabricar dados de potencia', async () => {
    const window = {};let chamadas = 0;
    vm.runInNewContext(codigo, {window, document: {addEventListener() {}},
        fetch: async () => { chamadas++;return {ok: chamadas > 1, json: async () => []}; }
    });
    await assert.rejects(window.carregarModelosImpressora());
    assert.deepEqual(await window.carregarModelosImpressora(), []);assert.equal(chamadas, 2);
});
