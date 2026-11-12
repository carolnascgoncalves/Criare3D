package br.com.criare3d.service;

import br.com.criare3d.models.CalculoHistorico;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class CalculoRascunhoMapperTest {
    @Test
    void historicoAntigoComOpcoesNulasGeraRascunhoValido() {
        var antigo = CalculoHistorico.builder().unidades(3).pecasPorImpressao(2)
                .tempoImpressaoMinutos(60L).markupPercentual(new BigDecimal("50"))
                .produzido(true).vendido(true).build();
        var copia = CalculoRascunhoMapper.copiar(antigo);
        assertThat(copia.getUnidades()).isEqualTo(3);
        assertThat(copia.getPecasPorImpressao()).isEqualTo(2);
        assertThat(copia.isTaxaCpfShopee()).isFalse();
        assertThat(copia.isFreteGratisTikTok()).isFalse();
        assertThat(copia.getPercentualImposto()).isZero();
        assertThat(copia.getFilamentos()).isEmpty();
        assertThat(copia.getFotoPecaBytes()).isNull();
    }
}
