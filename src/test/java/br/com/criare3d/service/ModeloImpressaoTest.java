package br.com.criare3d.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModeloImpressaoTest {

    @Test
    void distribuiDezUnidadesEmQuatroPecasPorImpressao() {
        assertThat(ModeloImpressao.quantidadeImpressoes(10, 4)).isEqualTo(3);
    }

    @Test
    void incluiAquecimentoEmCadaImpressao() {
        assertThat(ModeloImpressao.minutosTotais(240, 5, 3)).isEqualTo(735);
    }

    @Test
    void somaTempoDasDezImpressõesQuandoCadaUmaLevaDuasHoras() {
        int impressoes = ModeloImpressao.quantidadeImpressoes(10, 1);

        assertThat(impressoes).isEqualTo(10);
        assertThat(ModeloImpressao.minutosTotais(120, 0, impressoes)).isEqualTo(1200);
    }
}
