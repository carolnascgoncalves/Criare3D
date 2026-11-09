package br.com.criare3d.dto;

import br.com.criare3d.models.CalculoHistorico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoResponseTest {

    @Test
    void calculaMargemSobrePrecoNoDto() {
        CalculoHistorico calculo = new CalculoHistorico();
        calculo.setCustoTotal(new BigDecimal("75.00"));
        calculo.setPrecoSugerido(new BigDecimal("100.00"));

        assertThat(CalculoResponse.toDto(calculo).getMargemSobrePreco())
                .isEqualByComparingTo("25.0000");
    }

    @Test
    void margemFicaNulaQuandoPrecoNaoPodeSerDivisor() {
        CalculoHistorico calculo = new CalculoHistorico();
        calculo.setCustoTotal(new BigDecimal("1.00"));
        calculo.setPrecoSugerido(BigDecimal.ZERO);

        assertThat(CalculoResponse.toDto(calculo).getMargemSobrePreco()).isNull();
    }
}
