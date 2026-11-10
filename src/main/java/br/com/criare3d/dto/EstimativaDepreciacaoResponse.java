package br.com.criare3d.dto;

import java.math.BigDecimal;

public record EstimativaDepreciacaoResponse(
        boolean disponivel,
        String mensagem,
        String modelo,
        BigDecimal precoNovoMediano,
        int anunciosNovos,
        BigDecimal precoUsadoMediano,
        int anunciosUsados,
        BigDecimal horasAteSubstituicao,
        BigDecimal horasMensais,
        boolean horasMensaisPadrao,
        BigDecimal depreciacaoPorHora,
        boolean fallback,
        String confianca
) {}
