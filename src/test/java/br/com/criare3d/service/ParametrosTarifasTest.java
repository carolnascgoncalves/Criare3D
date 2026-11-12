package br.com.criare3d.service;

import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;

class ParametrosTarifasTest {
    private Properties padrao() throws Exception {
        var p = new Properties();
        try (var in = getClass().getResourceAsStream("/tarifas-plataformas.properties")) { p.load(in); }
        return p;
    }
    @Test void substituiTarifaSemAlterarAsDemais() throws Exception {
        var overrides = new Properties(); overrides.setProperty("TIKTOK_COMISSAO_PRIMEIRA_FAIXA", "11.25");
        var tabela = new ParametrosTarifas(padrao(), overrides);
        assertThat(tabela.decimal("TIKTOK_COMISSAO_PRIMEIRA_FAIXA")).isEqualByComparingTo("11.25");
        assertThat(tabela.decimal("SHOPEE_TAXA_CPF")).isEqualByComparingTo("3");
        assertThat(tabela.lista("ML_FRETES_ATE_300G", 4)).hasSize(4);
    }
    @Test void configuracaoInvalidaNaoPassaSilenciosamente() throws Exception {
        for (String[] item : new String[][]{{"TIKTOK_COMISSAO_PRIMEIRA_FAIXA", "101"},
                {"SHOPEE_TAXA_CPF", "-1"}, {"TAXA_DIGITADA_ERRADA", "2"},
                {"ML_LIMITES_PRECO", "99,79,100,119"}, {"ML_FRETES_ATE_300G", "1,2"},
                {"SHOPEE_INICIO_FAIXA_80", "81"}, {"ML_CLASSICO_COMISSAO_MINIMA", "15"}}) {
            var overrides = new Properties(); overrides.setProperty(item[0], item[1]);
            var defaults = padrao();
            assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosTarifas(defaults, overrides));
        }
    }
}
