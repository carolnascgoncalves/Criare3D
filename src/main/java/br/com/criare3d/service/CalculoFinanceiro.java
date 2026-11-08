package br.com.criare3d.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Fórmulas determinísticas usadas pelo cálculo de produção. */
final class CalculoFinanceiro {
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal MIL = new BigDecimal("1000");
    private static final BigDecimal SESSENTA = new BigDecimal("60");

    private CalculoFinanceiro() {}

    static BigDecimal energia(BigDecimal potenciaWatts, long minutos, BigDecimal tarifaKwh) {
        // Mantém a fração até a divisão final para não arredondar kWh no meio da fórmula.
        BigDecimal numerador = potenciaWatts.multiply(BigDecimal.valueOf(minutos)).multiply(tarifaKwh);
        return precisao(numerador.divide(MIL.multiply(SESSENTA), 12, RoundingMode.HALF_UP));
    }

    static BigDecimal filamento(BigDecimal quantidadeGramas, BigDecimal precoRolo,
                                BigDecimal pesoRoloGramas) {
        return precisao(quantidadeGramas.multiply(precoRolo).divide(pesoRoloGramas, 8,
                RoundingMode.HALF_UP));
    }

    static BigDecimal maoDeObra(BigDecimal valorHora, long minutos) {
        return precisao(valorHora.multiply(BigDecimal.valueOf(minutos))
                .divide(SESSENTA, 8, RoundingMode.HALF_UP));
    }

    static BigDecimal manutencao(BigDecimal valorHora, long minutos) {
        return maoDeObra(valorHora, minutos);
    }

    static BigDecimal depreciacao(BigDecimal valorCompra, BigDecimal vidaUtilHoras, long minutos) {
        BigDecimal horas = BigDecimal.valueOf(minutos).divide(SESSENTA, 8, RoundingMode.HALF_UP);
        return precisao(horas.multiply(valorCompra.divide(vidaUtilHoras, 8, RoundingMode.HALF_UP)));
    }

    static BigDecimal falhas(BigDecimal custoFilamento, BigDecimal custoEnergia, BigDecimal percentual) {
        return precisao(custoFilamento.add(custoEnergia).multiply(percentual)
                .divide(CEM, 8, RoundingMode.HALF_UP));
    }

    static BigDecimal ratear(BigDecimal custoTotal, int unidades) {
        return precisao(custoTotal.divide(BigDecimal.valueOf(unidades), 8, RoundingMode.HALF_UP));
    }

    static BigDecimal markup(BigDecimal custoTotal, BigDecimal percentual) {
        BigDecimal fator = BigDecimal.ONE.add(percentual.divide(CEM, 8, RoundingMode.HALF_UP));
        return moeda(custoTotal.multiply(fator));
    }

    static BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal precisao(BigDecimal valor) {
        return valor.setScale(8, RoundingMode.HALF_UP);
    }
}
