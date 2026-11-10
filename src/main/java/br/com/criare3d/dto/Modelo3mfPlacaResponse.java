package br.com.criare3d.dto;

import java.util.List;

public record Modelo3mfPlacaResponse(
        int numero,
        String imagemPreviewDataUri,
        Double tempoSegundos,
        Double pesoGramas,
        List<Modelo3mfFilamentoResponse> filamentos
) { }
