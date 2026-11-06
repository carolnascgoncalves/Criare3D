package br.com.criare3d.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TarifasPlataformaTest {
    private final CalculoHistoricoServiceImp calculo = new CalculoHistoricoServiceImp();

    @Test
    void shopeeAplicaFaixasEValorFixoVigentes() {
        assertThat(calculo.calcularTaxasShopee(
                new BigDecimal("79.99"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .isEqualByComparingTo("20.50");
        assertThat(calculo.calcularTaxasShopee(
                new BigDecimal("80.00"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .isEqualByComparingTo("27.20");
        assertThat(calculo.calcularTaxasShopee(
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .isEqualByComparingTo("34.00");
        assertThat(calculo.calcularTaxasShopee(
                new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .isEqualByComparingTo("54.00");
        assertThat(calculo.calcularTaxasShopee(
                new BigDecimal("8.00"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .isEqualByComparingTo("5.60");
        assertThat(calculo.calcularTaxasShopee(
                new BigDecimal("79.99"), BigDecimal.ZERO, BigDecimal.ZERO, true))
                .isEqualByComparingTo("23.50");
    }

    @Test
    void shopeeEncontraPrecoAbaixoDeNoveComTarifaFixaReduzida() {
        assertThat(calculo.calcularPrecoShopee(
                new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO, false))
                .isEqualByComparingTo("6.67");
    }

    @Test
    void tiktokAplicaComissaoETaxaPorItemNasDuasFaixas() {
        assertThat(calculo.calcularTaxasTikTok(
                new BigDecimal("45.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                false, BigDecimal.ZERO, false))
                .isEqualByComparingTo("8.50");
        assertThat(calculo.calcularTaxasTikTok(
                new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                false, BigDecimal.ZERO, false))
                .isEqualByComparingTo("9.00");
        assertThat(calculo.calcularTaxasTikTok(
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                true, BigDecimal.ZERO, false))
                .isEqualByComparingTo("18.00");
        assertThat(calculo.calcularTaxasTikTok(
                new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                false, BigDecimal.ZERO, true))
                .isEqualByComparingTo("6.00");
    }

    @Test
    void mercadoLivreMantemEstimativaDaFaixaConfiguradaERecusaAcimaDoLimite() {
        assertThat(calculo.calcularPrecoMercadoLivreSimples(
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("14.00"), "ATE_300G"))
                .isEqualByComparingTo("31.22");

        assertThatThrownBy(() -> calculo.calcularPrecoMercadoLivreSimples(
                new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("14.00"), "ATE_300G"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Informe o frete manualmente");
    }
}
