package br.com.criare3d.service;

import br.com.criare3d.models.ItemCalculoHistorico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraProducaoTest {

    @Test
    void distribuiPurgaEntreDoisFilamentosEConservaOTotal() {
        var primeiro = ItemCalculoHistorico.builder()
                .quantidadeFilamentoGramas(new BigDecimal("30.00")).build();
        var segundo = ItemCalculoHistorico.builder()
                .quantidadeFilamentoGramas(new BigDecimal("70.00")).build();

        DistribuidorPurga.distribuir(new BigDecimal("0.10000000"),
                new BigDecimal("100.00"), List.of(primeiro, segundo));

        assertThat(primeiro.getPurgaGramas()).isEqualByComparingTo("0.03000000");
        assertThat(segundo.getPurgaGramas()).isEqualByComparingTo("0.07000000");
        assertThat(primeiro.getPurgaGramas().add(segundo.getPurgaGramas()))
                .isEqualByComparingTo("0.10000000");
    }

    @Test
    void rateiaMaoDeObraEEnergiaDoLoteEntreDezUnidades() {
        CalculadoraProducao.Resultado resultado = CalculadoraProducao.calcular(
                new CalculadoraProducao.Entrada(10, new BigDecimal("2.00"), BigDecimal.ZERO,
                        new BigDecimal("0.00249333"), BigDecimal.ZERO, new BigDecimal("5.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                        new BigDecimal("100")));

        assertThat(resultado.custoMaoObra()).isEqualByComparingTo("0.50000000");
        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.00024933");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("2.50024933");
    }

    @Test
    void somaCustosDePurgaEnergiaEProducaoNoRateio() {
        CalculadoraProducao.Resultado resultado = CalculadoraProducao.calcular(
                new CalculadoraProducao.Entrada(2, new BigDecimal("3.00"), new BigDecimal("1.00"),
                        new BigDecimal("0.50"), new BigDecimal("0.25"), BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                        BigDecimal.ZERO));

        assertThat(resultado.custoPurga()).isEqualByComparingTo("0.50000000");
        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.25000000");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("4.00000000");
    }

    @Test
    void calculaPurgaTotalAPartirDaQuantidadePorImpressao() {
        assertThat(CalculadoraProducao.gramasPurgaLote(new BigDecimal("0.10"), 3))
                .isEqualByComparingTo("0.30000000");
        assertThat(CalculadoraProducao.custoPurgaLote(new BigDecimal("0.30"),
                new BigDecimal("8.00"), new BigDecimal("100.00")))
                .isEqualByComparingTo("0.02400000");
    }

    @Test
    void calculaCustosDaMaquinaComTempoTotalDoLote() {
        assertThat(CalculadoraProducao.energiaLote(new BigDecimal("100.00"), 120,
                new BigDecimal("1.00"))).isEqualByComparingTo("0.20000000");
        assertThat(CalculadoraProducao.manutencaoLote(new BigDecimal("0.20"), 120))
                .isEqualByComparingTo("0.40000000");
        assertThat(CalculadoraProducao.depreciacaoLote(new BigDecimal("2500.00"),
                new BigDecimal("5000.00"), 120)).isEqualByComparingTo("1.00000000");
    }

    @Test
    void calculaAsParcelasOperacionaisDoLoteEmUmSoLugar() {
        var custos = CalculadoraProducao.calcularCustosOperacionaisLote(
                new BigDecimal("100"), 120, new BigDecimal("1.00"),
                new BigDecimal("0.10"), 3, new BigDecimal("8.00"),
                new BigDecimal("100.00"), true, new BigDecimal("20.00"), 15,
                new BigDecimal("0.20"), true, new BigDecimal("2500.00"),
                new BigDecimal("5000.00"));

        assertThat(custos.energia()).isEqualByComparingTo("0.20000000");
        assertThat(custos.gramasPurga()).isEqualByComparingTo("0.30000000");
        assertThat(custos.custoPurga()).isEqualByComparingTo("0.02400000");
        assertThat(custos.maoDeObra()).isEqualByComparingTo("5.00000000");
        assertThat(custos.manutencao()).isEqualByComparingTo("0.40000000");
        assertThat(custos.depreciacao()).isEqualByComparingTo("1.00000000");
    }
}
