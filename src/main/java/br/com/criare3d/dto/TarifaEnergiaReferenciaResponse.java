package br.com.criare3d.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TarifaEnergiaReferenciaResponse(
        String distribuidora,
        BigDecimal valorKwh,
        LocalDate inicioVigencia,
        LocalDate fimVigencia,
        String unidade
) { }
