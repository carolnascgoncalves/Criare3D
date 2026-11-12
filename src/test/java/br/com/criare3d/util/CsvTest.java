package br.com.criare3d.util;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
class CsvTest {
    @Test void neutralizaFormulasInclusiveComEspacosETrocasDeLinha() {
        for (String formula : new String[]{"=SUM(A1)", "+1", "-1", "@SUM(A1)", "\t =1\n+2"})
            assertThat(Csv.campo(formula)).startsWith("\"'");
    }
    @Test void preservaAcentosEscapaAspasEFormataDecimal() {
        assertThat(Csv.campo("Peça \"azul\"; grande")).isEqualTo("\"Peça \"\"azul\"\"; grande\"");
        assertThat(Csv.numero(new BigDecimal("95.50"))).isEqualTo("95,50");
        assertThat(Csv.campo(null)).isEqualTo("\"\"");
    }
}
