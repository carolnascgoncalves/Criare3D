package br.com.criare3d.dto;

import java.math.BigDecimal;

/** Projection tipada do resumo agregado nativo da dashboard. */
public interface DashboardResumoProjection {
    Number getCalculadas();
    Number getProduzidas();
    Number getVendidas();
    BigDecimal getLucro();
    Number getMinutosVendidos();
    Number getMinutosProduzidos();
    BigDecimal getGramasFilamento();
}
