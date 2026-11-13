package br.com.criare3d.service;

import br.com.criare3d.models.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class RateioCustosTest {
    private CustoLoja custo(CustoLoja.Tipo tipo, String valor) {
        var c=new CustoLoja(); c.setDescricao("Despesas novembro"); c.setTipo(tipo);
        c.setMeta(new BigDecimal("1500"));c.setValor(new BigDecimal(valor));c.setUnidadesPrevistas(300);return c;
    }
    @Test void cincoReaisPorUnidadeDezVendasRecuperamCinquenta() {
        var c=custo(CustoLoja.Tipo.VALOR_UNIDADE,"5");
        var parcela=RateioCustos.porUnidade(c,new BigDecimal("20"));
        assertThat(parcela.multiply(BigDecimal.TEN)).isEqualByComparingTo("50");
        assertThat(RateioCustos.falta(c.getMeta(),parcela.multiply(BigDecimal.TEN))).isEqualByComparingTo("1450");
        assertThat(RateioCustos.falta(c.getMeta(),parcela.multiply(new BigDecimal("8")))).isEqualByComparingTo("1460");
        assertThat(RateioCustos.falta(c.getMeta(),new BigDecimal("1600"))).isZero();
    }
    @Test void percentualUsaMesmaBaseSemComporComOutrosRateios() {
        var h=new CalculoHistorico();h.setCustoTotal(new BigDecimal("20"));h.setMarkupPercentual(new BigDecimal("30"));
        var fixo=custo(CustoLoja.Tipo.VALOR_UNIDADE,"5");var percentual=custo(CustoLoja.Tipo.PERCENTUAL,"10");
        RateioCustos.aplicar(h,List.of(fixo,percentual));
        assertThat(h.getCustoLoja()).isEqualByComparingTo("7");assertThat(h.getCustoTotal()).isEqualByComparingTo("27");
        assertThat(h.getPrecoSugerido()).isEqualByComparingTo("35.10");
        fixo.setValor(new BigDecimal("100"));fixo.setDescricao("Outro período");
        assertThat(h.getRateiosCustos().getFirst().getValorUnitario()).isEqualByComparingTo("5");
        assertThat(h.getRateiosCustos().getFirst().getDescricao()).isEqualTo("Despesas novembro");
    }
    @Test void rateioMensalDivideMetaPelasUnidadesSemDependerDoCustoDaPeca() {
        assertThat(RateioCustos.porUnidade(custo(CustoLoja.Tipo.RATEIO_MENSAL,"0"),new BigDecimal("20"))).isEqualByComparingTo("5");
    }
}
