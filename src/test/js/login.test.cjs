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
        addEventListener: (nome, callback) => { eventos[nome] = callback; },
        querySelector: seletor => seletor === '[data-login-espera]' ? aviso : botao
    };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/login.js', 'utf8'), {
        document, Date: {now: () => agora},
        setInterval: callback => { atualizar = callback; return 1; },
        clearInterval: () => {}
    });
    eventos.DOMContentLoaded();
    assert.equal(botao.disabled, true);
    assert.equal(segundos.textContent, '5');
    agora += 6000;
    atualizar();
    assert.equal(botao.disabled, false);
    assert.match(aviso.textContent, /já pode tentar entrar novamente/);
});
