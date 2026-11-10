package br.com.criare3d.dto;

public record Modelo3mfResponse(
        String nome,
        String modeloImpressora,
        String material,
        String corMaterial,
        String alturaCamada,
        String diametroBico,
        String perfil,
        String fatiador,
        boolean jaFatiado,
        boolean temEstatisticas,
        boolean estatisticasCompletas,
        int numeroPlacasComEstatisticas,
        Double tempoTotalSegundos,
        Double pesoTotalGramas,
        java.util.List<Modelo3mfFilamentoResponse> filamentosUsados,
        int numeroPlacas,
        java.util.List<Modelo3mfPlacaResponse> placas
) { }
