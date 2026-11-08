package br.com.criare3d.dto;

import java.math.BigDecimal;

public record DashboardResumo(
        long calculadas,
        long produzidas,
        long vendidas,
        BigDecimal lucroVendido,
        long minutosVendidos,
        long minutosProduzidos,
        BigDecimal gramasFilamento
) {
    public static DashboardResumo from(DashboardResumoProjection valores) {
        return new DashboardResumo(
                numero(valores.getCalculadas()), numero(valores.getProduzidas()), numero(valores.getVendidas()),
                valores.getLucro(), numero(valores.getMinutosVendidos()),
                numero(valores.getMinutosProduzidos()), valores.getGramasFilamento());
    }

    private static long numero(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0L;
    }

}
