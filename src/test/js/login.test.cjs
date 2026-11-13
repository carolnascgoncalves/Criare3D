const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

test('bloqueio fica no aviso e libera o botão quando a espera termina', () => {
    let agora = 1000;
    let atualizar;
    const eventos = {};
    const botao = {disabled: false};
    const segundos = {textContent: ''};
    const aviso = {dataset: {loginEspera: '5'}, querySelector: () => segundos};
    const document = {
        addEventListener: (nome, callback) => { (eventos[nome] ??= []).push(callback); },
        querySelectorAll: () => [],
        querySelector: seletor => seletor === '[data-login-espera]' ? aviso : botao
    };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/login.js', 'utf8'), {
        document, Date: {now: () => agora},
        setInterval: callback => { atualizar = callback; return 1; },
        clearInterval: () => {}, setTimeout: () => 1, clearTimeout: () => {}
    });
    eventos.DOMContentLoaded.forEach(callback => callback());
    assert.equal(botao.disabled, true);
    assert.equal(segundos.textContent, '5');
    agora += 6000;
    atualizar();
    assert.equal(botao.disabled, false);
    assert.match(aviso.textContent, /já pode tentar entrar novamente/);
});

test('logout some sem esconder erros e cancela o timer ao sair da página', () => {
    const eventos = {};
    const aviso = {hidden: false};
    const erro = {hidden: false};
    const timers = new Map();
    let seq = 0;
    const document = {
        addEventListener: (nome, callback) => { (eventos[nome] ??= []).push(callback); },
        querySelector: () => null,
        querySelectorAll: seletor => seletor === '[data-aviso-temporario]' ? [aviso] : []
    };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/login.js', 'utf8'), {
        document, Date, setInterval: () => 1, clearInterval: () => {},
        setTimeout: (callback, delay) => { assert.equal(delay, 7000); timers.set(++seq, callback); return seq; },
        clearTimeout: id => timers.delete(id)
    });
    eventos.DOMContentLoaded.forEach(callback => callback());
    eventos['turbo:load'].forEach(callback => callback());
    assert.equal(timers.size, 1);
    const [id, callback] = [...timers.entries()][0];
    timers.delete(id);
    callback();
    assert.equal(aviso.hidden, true);
    assert.equal(erro.hidden, false);
    aviso.hidden = false;
    eventos['turbo:load'].forEach(callback => callback());
    eventos['turbo:before-cache'].forEach(callback => callback());
    assert.equal(aviso.hidden, true);
    assert.equal(timers.size, 0);
});
