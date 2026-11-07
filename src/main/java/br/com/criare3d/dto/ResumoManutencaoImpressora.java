package br.com.criare3d.dto;

import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.ManutencaoImpressora;

import java.math.BigDecimal;
import java.util.List;

public record ResumoManutencaoImpressora(
        Impressora impressora,
        List<ManutencaoImpressora> registros,
        BigDecimal totalGasto,
        long minutosImpressos,
        BigDecimal custoPorHora
) {}
