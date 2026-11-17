package br.com.criare3d.service;

import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.CustoLoja;
import br.com.criare3d.models.RateioCustoHistorico;

import java.math.BigDecimal;
import java.util.List;

public final class RateioCustos {
    private RateioCustos() {
    }

    public static BigDecimal porUnidade(CustoLoja custo, BigDecimal base) {
        return RateioCalculo.porUnidade(custo.getTipo().name(), custo.getValor(), custo.getMeta(),
            custo.getUnidadesPrevistas(), base);
    }

    public static void aplicar(CalculoHistorico calculo, List<CustoLoja> custos) {
        var estado = new br.com.criare3d.dto.calculo.EstadoCalculo();
        estado.setCustoTotal(calculo.getCustoTotal());
        estado.setMarkupPercentual(calculo.getMarkupPercentual());
        var entradas = custos.stream().map(c -> new br.com.criare3d.dto.calculo.EntradaCalculo.CustoDados(
            c.getId(), c.getDescricao(), c.getTipo().name(), c.getValor(), c.getMeta(), c.getUnidadesPrevistas())).toList();
        RateioCalculo.aplicar(estado, entradas);
        for (int indice = 0; indice < custos.size(); indice++) {
            var resultado = estado.getRateiosCustos().get(indice);
            var item = new RateioCustoHistorico();
            item.setCalculo(calculo);
            item.setCustoLoja(custos.get(indice));
            item.setDescricao(resultado.getDescricao());
            item.setValorUnitario(resultado.getValorUnitario());
            calculo.getRateiosCustos().add(item);
        }
        calculo.setCustoLoja(estado.getCustoLoja());
        calculo.setCustoTotal(estado.getCustoTotal());
        calculo.setPrecoSugerido(estado.getPrecoSugerido());
    }

    public static BigDecimal falta(BigDecimal meta, BigDecimal recuperado) {
        return meta.subtract(recuperado).max(BigDecimal.ZERO);
    }
}
