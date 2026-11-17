package br.com.criare3d.service;

import br.com.criare3d.dto.calculo.EntradaCalculo.CustoDados;
import br.com.criare3d.dto.calculo.EstadoCalculo;
import br.com.criare3d.dto.calculo.RateioMotor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Rateios da simulação, sem criar relacionamentos JPA. */
public final class RateioCalculo {
    private RateioCalculo() {
    }

    public static void aplicar(EstadoCalculo calculo, List<CustoDados> custos) {
        var base = calculo.getCustoTotal();
        var total = BigDecimal.ZERO;
        for (var custo : custos) {
            var valor = porUnidade(custo.tipo(), custo.valor(), custo.meta(), custo.unidadesPrevistas(), base);
            calculo.getRateiosCustos().add(new RateioMotor(custo.id(), custo.descricao(), valor));
            total = total.add(valor);
        }
        calculo.setCustoLoja(total);
        calculo.setCustoTotal(base.add(total));
        calculo.setPrecoSugerido(calculo.getCustoTotal().multiply(BigDecimal.ONE.add(
            calculo.getMarkupPercentual().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)))
            .setScale(2, RoundingMode.HALF_UP));
    }
    public static BigDecimal porUnidade(String tipo, BigDecimal valor, BigDecimal meta, Integer unidades, BigDecimal base) {
        return switch (tipo) {
            case "VALOR_UNIDADE" -> valor;
            case "PERCENTUAL" -> base.multiply(valor).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
            case "RATEIO_MENSAL" -> meta.divide(BigDecimal.valueOf(unidades), 8, RoundingMode.HALF_UP);
            default -> throw new IllegalArgumentException("Tipo de custo inválido.");
        };
    }
}
