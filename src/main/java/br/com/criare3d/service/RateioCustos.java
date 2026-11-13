package br.com.criare3d.service;
import br.com.criare3d.models.*;
import java.math.*;
import java.util.List;

public final class RateioCustos {
    private RateioCustos() {}
    public static BigDecimal porUnidade(CustoLoja custo, BigDecimal base) {
        return switch(custo.getTipo()) {
            case VALOR_UNIDADE -> custo.getValor();
            case PERCENTUAL -> base.multiply(custo.getValor()).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
            case RATEIO_MENSAL -> custo.getMeta().divide(BigDecimal.valueOf(custo.getUnidadesPrevistas()), 8, RoundingMode.HALF_UP);
        };
    }
    public static void aplicar(CalculoHistorico calculo, List<CustoLoja> custos) {
        BigDecimal base = calculo.getCustoTotal();
        BigDecimal total = BigDecimal.ZERO;
        for (CustoLoja custo : custos) {
            var item = new RateioCustoHistorico();
            item.setCalculo(calculo); item.setCustoLoja(custo); item.setDescricao(custo.getDescricao());
            item.setValorUnitario(porUnidade(custo, base));
            calculo.getRateiosCustos().add(item);
            total = total.add(item.getValorUnitario());
        }
        calculo.setCustoLoja(total);
        calculo.setCustoTotal(base.add(total));
        calculo.setPrecoSugerido(calculo.getCustoTotal().multiply(BigDecimal.ONE.add(
                calculo.getMarkupPercentual().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP))).setScale(2, RoundingMode.HALF_UP));
    }
    public static BigDecimal falta(BigDecimal meta, BigDecimal recuperado) {
        return meta.subtract(recuperado).max(BigDecimal.ZERO);
    }
}
