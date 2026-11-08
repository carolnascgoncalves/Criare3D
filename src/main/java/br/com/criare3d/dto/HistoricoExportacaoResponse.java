package br.com.criare3d.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoExportacaoResponse(
        String nomePeca,
        LocalDateTime dataCalculo,
        String plataforma,
        BigDecimal custoTotal,
        BigDecimal precoSugerido,
        BigDecimal lucroLiquido,
        Boolean produzido,
        Boolean vendido,
        Integer unidades,
        Integer fornadas
) { }
