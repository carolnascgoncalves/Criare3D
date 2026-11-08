package br.com.criare3d.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoFinanceiroTest {

    @Test
    void somaCustosDeMaisDeUmFilamentoComArredondamentoPorItem() {
        BigDecimal primeiro = CalculoFinanceiro.filamento(
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("1000"));
        BigDecimal segundo = CalculoFinanceiro.filamento(
                new BigDecimal("50"), new BigDecimal("75"), new BigDecimal("750"));

        assertThat(primeiro).isEqualByComparingTo("10.00");
        assertThat(segundo).isEqualByComparingTo("5.00");
        assertThat(CalculoFinanceiro.moeda(primeiro.add(segundo)))
                .isEqualByComparingTo("15.00");
    }

    @Test
    void mantémPrecisaoInternaEApenasArredondaOResultadoFinal() {
        BigDecimal filamento = new BigDecimal("10.00");
        BigDecimal energia = CalculoFinanceiro.energia(
                new BigDecimal("220"), 300, new BigDecimal("0.68"));
        BigDecimal embalagem = new BigDecimal("2.00");
        BigDecimal falha = CalculoFinanceiro.falhas(filamento, energia, new BigDecimal("10"));
        BigDecimal maoDeObra = CalculoFinanceiro.maoDeObra(new BigDecimal("20"), 15);
        BigDecimal depreciacao = CalculoFinanceiro.depreciacao(
                new BigDecimal("2500"), new BigDecimal("5000"), 300);
        BigDecimal manutencao = CalculoFinanceiro.manutencao(new BigDecimal("0.20"), 300);

        BigDecimal total = filamento.add(energia).add(embalagem)
                .add(falha).add(maoDeObra).add(depreciacao).add(manutencao);

        assertThat(CalculoFinanceiro.moeda(energia)).isEqualByComparingTo("0.75");
        assertThat(CalculoFinanceiro.moeda(falha)).isEqualByComparingTo("1.07");
        assertThat(maoDeObra).isEqualByComparingTo("5.00");
        assertThat(depreciacao).isEqualByComparingTo("2.50");
        assertThat(manutencao).isEqualByComparingTo("1.00");
        assertThat(CalculoFinanceiro.moeda(total)).isEqualByComparingTo("22.32");
        assertThat(CalculoFinanceiro.markup(total, new BigDecimal("100")))
                .isEqualByComparingTo("44.65");
    }

    @Test
    void rateiaMaoDeObraDoLotePorDezUnidadesSemMultiplicarOCusto() {
        BigDecimal maoDeObraDoLote = CalculoFinanceiro.maoDeObra(new BigDecimal("20"), 15);
        BigDecimal maoDeObraPorUnidade = CalculoFinanceiro.ratear(maoDeObraDoLote, 10);

        assertThat(maoDeObraPorUnidade).isEqualByComparingTo("0.50000000");
        assertThat(maoDeObraPorUnidade.multiply(BigDecimal.TEN))
                .isEqualByComparingTo("5.00000000");
    }

    @Test
    void conservaCentavosFracionariosDeEnergiaAoRatearUmLote() {
        BigDecimal custoEnergiaLote = CalculoFinanceiro.energia(
                new BigDecimal("220"), 1, new BigDecimal("0.68"));
        BigDecimal custoPorUnidade = CalculoFinanceiro.ratear(custoEnergiaLote, 10);

        assertThat(custoEnergiaLote).isEqualByComparingTo("0.00249333");
        assertThat(custoPorUnidade).isEqualByComparingTo("0.00024933");
        assertThat(CalculoFinanceiro.moeda(custoPorUnidade)).isEqualByComparingTo("0.00");
        assertThat(custoPorUnidade.multiply(BigDecimal.TEN))
                .isEqualByComparingTo("0.00249330");
    }

    @Test
    void manutencaoContinuaSendoCalculadaSemDepreciacao() {
        assertThat(CalculoFinanceiro.manutencao(new BigDecimal("0.20"), 300))
                .isEqualByComparingTo("1.00");
        assertThat(CalculoFinanceiro.depreciacao(new BigDecimal("0"), new BigDecimal("5000"), 300))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void arredondaMeioCentavoParaCima() {
        assertThat(CalculoFinanceiro.moeda(new BigDecimal("1.075")))
                .isEqualByComparingTo("1.08");
    }

    @Test
    void calculaTresImpressoesParaDezUnidadesComQuatroPecasPorImpressao() {
        int impressoes = ModeloImpressao.quantidadeImpressoes(10, 4);

        assertThat(impressoes).isEqualTo(3);
        assertThat(ModeloImpressao.minutosTotais(240, 5, impressoes)).isEqualTo(735);
    }

    @Test
    void calculadoraRateiaMaoDeObraEConservaEnergiaFracionariaDoLote() {
        CalculadoraProducao.Resultado resultado = CalculadoraProducao.calcular(
                new CalculadoraProducao.Entrada(10, new BigDecimal("2.00"), BigDecimal.ZERO,
                        new BigDecimal("0.00249333"), BigDecimal.ZERO, new BigDecimal("5.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                        new BigDecimal("100")));

        assertThat(resultado.custoMaoObra()).isEqualByComparingTo("0.50000000");
        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.00024933");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("2.50024933");
        assertThat(resultado.precoSugerido()).isEqualByComparingTo("5.00");
    }
}
