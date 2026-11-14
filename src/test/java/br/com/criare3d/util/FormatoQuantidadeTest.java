package br.com.criare3d.util;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
class FormatoQuantidadeTest {
    @Test void gramasAcimaDeMilViramQuilos() {
        assertThat(FormatoQuantidade.resumido(new BigDecimal("5415.300000"),"g",true)).isEqualTo("+5,42 kg");
        assertThat(FormatoQuantidade.resumido(new BigDecimal("-1800"),"g",false)).isEqualTo("-1,80 kg");
    }
    @Test void gramasPequenosTemNoMaximoUmaCasa() {
        assertThat(FormatoQuantidade.resumido(new BigDecimal("-86.250000"),"g",true)).isEqualTo("-86,3 g");
        assertThat(FormatoQuantidade.resumido(new BigDecimal("12"),"g",true)).isEqualTo("+12 g");
    }
    @Test void unidadesNaoTemCasasDecimais() {
        assertThat(FormatoQuantidade.resumido(new BigDecimal("-2.000000"),"un.",true)).isEqualTo("-2 un.");
    }
    @Test void valorExatoMantemTresCasasEmGramas() {
        assertThat(FormatoQuantidade.exato(new BigDecimal("5415.3"),"g")).isEqualTo("+5.415,300 g");
    }
}
