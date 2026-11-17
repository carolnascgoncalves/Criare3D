const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const codigo = fs.readFileSync('src/main/resources/static/js/catalogoBusca.js', 'utf8');

function preparar(tipo, modo = 'normal', respostaPost) {
    const eventos = new Map();
    class Elemento {
        constructor(tag) {
            this.tagName = tag; this.children = []; this.dataset = {}; this.eventos = new Map();
            this.className = ''; this.value = ''; this.validity = {valid: true};
            this.classList = {add() {}, contains: nome => this.className.split(' ').includes(nome)};
        }
        addEventListener(nome, callback) { this.eventos.set(nome, callback); }
        setAttribute(nome, valor) { this[nome] = valor; }
        removeAttribute(nome) { delete this[nome]; }
        append(...filhos) { this.children.push(...filhos); }
        replaceChildren(...filhos) { this.children = filhos; }
        dispatchEvent(evento) { this.eventos.get(evento.type)?.(evento); }
        focus() {}
        showModal() {}
        remove() {}
        click() { return this.eventos.get('click')?.(); }
        close() { this.fechado = true; this.eventos.get('close')?.(); }
    }
    const select = new Elemento('select');
    select.dataset.catalogo = tipo; select.value = 'material-anterior'; select.options = [];
    select.selectedOptions = [{textContent: 'Cadastro selecionado', dataset: {}}];
    select.closest = seletor => modo && (modo === 'normal' ? seletor.includes('#formCalculo')
        : seletor.includes('[data-calculo-rapido]')) ? {} : null;
    select.matches = () => false;
    select.insertAdjacentElement = (_, elemento) => { select.nextElementSibling = elemento; };
    select.append = opcao => select.options.push(opcao);
    const body = new Elemento('body');
    const document = {
        body,
        querySelectorAll: seletor => seletor === 'select[data-catalogo]' ? [select]
            : seletor.startsWith('meta') ? [{content: 'csrf-antigo'}, {content: 'csrf-atual'}] : [],
        createElement: tag => new Elemento(tag),
        addEventListener: (nome, callback) => eventos.set(nome, callback)
    };
    const requests = [];
    const item = {id: 'novo-id', nome: 'Novo cadastro', atributos: {codigoCor: '#ffee00', cor: 'Amarelo',
        pesoRolo: 1000, precoRolo: 80, estoque: 0, potencia: 100, custo: 2.5}};
    const sucesso = () => ({ok: true, headers: {get: () => 'application/json'}, json: async () => item});
    const window = {carregarModelosImpressora: async () => [{nome: 'Bambu Lab A1', potenciaMediaPLA: 95}]};
    vm.runInNewContext(codigo, {document, window, Intl, URLSearchParams, AbortController,
        setTimeout, clearTimeout, Event,
        MutationObserver: class {observe() {} disconnect() {}},
        fetch: async (url, options) => {
            requests.push({url, options});
            if (options?.method === 'POST') return respostaPost ? respostaPost() : sucesso();
            return {ok: true, json: async () => ({itens: [], mais: false})};
        }
    });
    eventos.get('DOMContentLoaded')(); select.nextElementSibling.click();
    const dialogo = body.children[0];
    const descendentes = raiz => raiz.children.flatMap(e => [e, ...descendentes(e)]);
    const cadastro = descendentes(dialogo).find(e => e.textContent?.startsWith('Cadastrar '));
    const abrir = async () => {
        cadastro.click();
        await new Promise(setImmediate);
        const form = dialogo.children.find(e => e.tagName === 'form');
        descendentes(form).filter(e => e.tagName === 'input').forEach(input => {
            if (!input.value) input.value = input.type === 'number' ? '80.50' : 'Novo cadastro';
        });
        const modelo = descendentes(form).find(e => e.name === 'modelo');
        if (modelo) modelo.value = 'Bambu Lab A1';
        return form;
    };
    return {select, dialogo, cadastro, requests, abrir, descendentes,
        direto: () => { window.abrirCadastroCatalogo(select); return body.children.at(-1); },
        enviar: form => form.eventos.get('submit')({preventDefault() {}})};
}

for (const modo of ['normal', 'rapido']) {
    for (const tipo of ['filamento', 'impressora', 'embalagem']) {
        test(`cadastro de ${tipo} no calculo ${modo} salva e seleciona no mesmo painel`, async () => {
            const a = preparar(tipo, modo);
            assert.equal(a.cadastro.tagName, 'button');
            const form = await a.abrir();
            await a.enviar(form);
            const request = a.requests.find(r => r.options?.method === 'POST');
            assert.equal(request.url, `/catalogo-opcoes/cadastro/${tipo}`);
            assert.equal(request.options.headers['X-CSRF-TOKEN'], 'csrf-atual');
            assert.equal(a.select.value, 'novo-id');
            assert.equal(a.select.options[0].dataset.precoRolo, 80);
            assert.equal(a.dialogo.fechado, true);
            if (tipo === 'filamento' && modo === 'rapido') assert.equal(a.select.options[0].dataset.cor, '#ffee00');
        });
    }
}

test('erro do servidor preserva campos e selecao anterior e cancelar nao cria item', async () => {
    const a = preparar('filamento', 'normal', () => ({ok: false, headers: {get: () => 'application/json'},
        json: async () => ({mensagem: 'Confira os dados.', campos: {nome: 'Nome obrigatório'}})}));
    const form = await a.abrir();
    await a.enviar(form);
    assert.equal(a.dialogo.fechado, undefined);
    assert.equal(a.select.value, 'material-anterior');
    const nome = a.descendentes(form).find(e => e.name === 'nome');
    assert.equal(nome.value, 'Novo cadastro');
    assert.equal(nome['aria-invalid'], 'true');
    assert.ok(a.descendentes(form).some(e => e.textContent === 'Nome obrigatório' && !e.hidden));
    a.descendentes(form).find(e => e.textContent === 'Cancelar').click();
    assert.equal(a.dialogo.fechado, true);
    assert.equal(a.select.options.length, 0);
});

test('quantidade de filamento e enviada e modelo conhecido dispensa potencia manual', async () => {
    const filamento = preparar('filamento');
    const form = await filamento.abrir();
    filamento.descendentes(form).find(e => e.name === 'estoqueGramas').value = '450.5';
    await filamento.enviar(form);
    assert.equal(JSON.parse(filamento.requests.find(r => r.options?.method === 'POST').options.body).estoqueGramas, '450.5');
    const impressora = preparar('impressora');
    const formulario = await impressora.abrir();
    assert.equal(impressora.descendentes(formulario).find(e => e.name === 'potenciaWatts').disabled, true);
    await impressora.enviar(formulario);
    const dados = JSON.parse(impressora.requests.find(r => r.options?.method === 'POST').options.body);
    assert.equal(dados.modelo, 'Bambu Lab A1');
    assert.equal(dados.potenciaWatts, undefined);
});

test('outro modelo mostra campos manuais e envia o nome informado', async () => {
    const a = preparar('impressora');
    const form = await a.abrir();
    const campos = a.descendentes(form);
    const modelo = campos.find(e => e.name === 'modelo');
    modelo.value = '__outro__';modelo.dispatchEvent(new Event('change'));
    campos.find(e => e.name === 'modeloPersonalizado').value = 'Minha impressora';
    const potencia = campos.find(e => e.name === 'potenciaWatts');
    assert.equal(potencia.disabled, false);potencia.value = '120';
    await a.enviar(form);
    const dados = JSON.parse(a.requests.find(r => r.options?.method === 'POST').options.body);
    assert.equal(dados.modelo, 'Minha impressora');assert.equal(dados.potenciaWatts, '120');
    assert.equal(dados.modeloPersonalizado, undefined);
});

test('duplo envio e Escape durante o salvamento nao duplicam nem descartam o cadastro', async () => {
    let concluir;
    const a = preparar('embalagem', 'rapido', () => new Promise(resolve => { concluir = resolve; }));
    const form = await a.abrir();
    const pendente = a.enviar(form);
    await a.enviar(form);
    let impedido = false;
    a.dialogo.eventos.get('cancel')({preventDefault() { impedido = true; }});
    assert.equal(impedido, true);
    assert.equal(a.requests.filter(r => r.options?.method === 'POST').length, 1);
    concluir({ok: true, headers: {get: () => 'application/json'},
        json: async () => ({id: 'novo', nome: 'Caixa', atributos: {custo: 2}})});
    await pendente;
    assert.equal(a.select.value, 'novo');
});

test('atalho antigo de cadastrar tambem abre painel e cancelar preserva a selecao anterior', () => {
    const a = preparar('filamento');
    a.dialogo.close();
    a.select.selectedOptions = [{dataset: {acao: 'cadastrar-filamento'}}];
    a.select.value = '';
    const dialogo = a.direto();
    assert.equal(a.select.value, 'material-anterior');
    a.descendentes(dialogo).find(e => e.textContent === 'Cancelar').click();
    assert.equal(a.select.value, 'material-anterior');
    assert.equal(dialogo.fechado, true);
});

test('sessao expirada e falha de rede deixam o erro no painel sem alterar o calculo', async () => {
    for (const resposta of [() => ({redirected: true}), () => { throw new Error('Falha de rede'); }]) {
        const a = preparar('impressora', 'normal', resposta);
        const form = await a.abrir(); await a.enviar(form);
        assert.equal(a.dialogo.fechado, undefined);
        assert.equal(a.select.value, 'material-anterior');
        assert.equal(form.children[0].hidden, false);
    }
    assert.equal(preparar('filamento', null).cadastro, undefined);
});
