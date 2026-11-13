const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const contexto = {document: {addEventListener() {}}};
vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/calculoRapido.js','utf8'), contexto);
const preparar = contexto.prepararPlacasConsultaRapida;
test('consulta importa todos os três filamentos com gramas na placa única', () => {
    const filamentos = [{id:1,pesoGramas:12},{id:2,pesoGramas:24},{id:3,pesoGramas:36}];
    const [placa] = preparar({numeroPlacas:1,placas:[{numero:1,quantidadePecas:2,imagemPreviewDataUri:'imagem',filamentos:[]}],filamentosUsados:filamentos,tempoTotalSegundos:3600,pesoTotalGramas:72});
    assert.deepEqual(placa.filamentos, filamentos);
    assert.equal(placa.quantidadePecas,2);assert.equal(placa.imagemPreviewDataUri,'imagem');assert.equal(placa.tempoSegundos,3600);
});
test('trocar de placa preserva filamentos próprios sem misturar os totais de outras placas', () => {
    const placas = preparar({numeroPlacas:2,tempoTotalSegundos:6000,filamentosUsados:[{id:1,pesoGramas:100}],placas:[{numero:1,filamentos:[{id:2,pesoGramas:20}],tempoSegundos:1000},{numero:2,filamentos:[]}]});
    assert.equal(placas[0].filamentos[0].pesoGramas,20);assert.equal(placas[1].filamentos.length,0);assert.equal(placas[1].tempoSegundos,null);
});

test('totais somam todas as placas e materiais sem multiplicar o tempo pelas peças', () => {
    const resumo=contexto.resumirArquivoConsultaRapida({numeroPlacas:2,placas:[
        {numero:1,quantidadePecas:2,tempoSegundos:600,filamentos:[{id:1,material:'PLA',cor:'#000000',pesoGramas:10}]},
        {numero:2,quantidadePecas:3,tempoSegundos:1200,filamentos:[{id:1,material:'PLA',cor:'#000000',pesoGramas:20},{id:2,pesoGramas:5}]}
    ]});
    assert.equal(resumo.quantidadePecas,5);assert.equal(resumo.tempoSegundos,1800);assert.equal(resumo.numeroPlacas,2);
    assert.equal(resumo.filamentos.length,2);assert.equal(resumo.filamentos[0].pesoGramas,30);
});
test('estatísticas incompletas não são apresentadas como totais completos', () => {
    const resumo=contexto.resumirArquivoConsultaRapida({numeroPlacas:2,tempoTotalSegundos:600,placas:[{numero:1,quantidadePecas:2,tempoSegundos:600},{numero:2}]});
    assert.equal(resumo.quantidadePecas,null);assert.equal(resumo.tempoSegundos,null);
});

test('unidades completas multiplicam os gramas sem dividir pelas partes do arquivo', () => {
    const base=5.57;
    assert.equal(contexto.escalarConsumoConsultaRapida(base,1),5.57);
    assert.equal(contexto.escalarConsumoConsultaRapida(base,2),11.14);
    assert.equal(contexto.escalarConsumoConsultaRapida(base,3),16.71);
    assert.equal(contexto.escalarConsumoConsultaRapida(base,1),5.57);
});

test('a primeira visita inicializa mesmo quando Turbo carrega o script depois dos eventos', () => {
    let consultas=0;
    const document={readyState:'complete',addEventListener(){},querySelector(){consultas++;return null;}};
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/calculoRapido.js','utf8'),{document});
    assert.equal(consultas,1);
});
test('o envio prioriza o token do formulário atual e usa o último token do cabeçalho como alternativa', () => {
    const doc={querySelectorAll:()=>[{content:'anterior'},{content:'atual'}]};
    assert.equal(contexto.tokenConsultaRapida({querySelector:()=>({value:'formulario-atual'})},doc),'formulario-atual');
    assert.equal(contexto.tokenConsultaRapida({querySelector:()=>null},doc),'atual');
});
