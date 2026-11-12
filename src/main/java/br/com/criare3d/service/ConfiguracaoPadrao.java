package br.com.criare3d.service;
import br.com.criare3d.models.Configuracao;
import java.math.BigDecimal;
/** Mesmos padrões do cadastro; permite calcular sem criar registros. */
public final class ConfiguracaoPadrao {
    private ConfiguracaoPadrao() {}
    public static Configuracao criar() {
            Configuracao configuracao = new Configuracao();

            configuracao.setMarkupPercentual(BigDecimal.ZERO);
            configuracao.setMarkupFixo(false);
            configuracao.setMarkupOpcao1(new BigDecimal("30.00"));
            configuracao.setMarkupOpcao2(new BigDecimal("40.00"));
            configuracao.setMarkupOpcao3(new BigDecimal("50.00"));
            configuracao.setTarifaEnergiaKwh(BigDecimal.ZERO);
            configuracao.setEmbalagemFixa(false);
            configuracao.setCustoEmbalagemFixa(BigDecimal.ZERO);
            configuracao.setFormularioAvancado(false);
            configuracao.setMaoObraAtivada(false);
            configuracao.setMaoObraTempoFixo(false);
            configuracao.setTempoMaoObraFixoMinutos(0L);
            configuracao.setDepreciacaoAtivada(false);
            configuracao.setTaxaFalhaAtivada(false);
            configuracao.setPercentualFalha(new BigDecimal("5.00"));
            configuracao.setPurgaGramasPorImpressao(BigDecimal.ZERO);

        return configuracao;
    }
}
