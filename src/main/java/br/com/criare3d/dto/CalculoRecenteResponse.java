package br.com.criare3d.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Campos leves usados nos cartões de cálculos recentes da Home e da dashboard. */
public record CalculoRecenteResponse(
        UUID id,
        String nomePeca,
        BigDecimal custoTotal,
        BigDecimal precoSugerido,
        String plataformaVenda,
        Boolean produzido,
        Boolean vendido,
        Integer unidades
) {}
