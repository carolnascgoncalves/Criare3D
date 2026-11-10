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
        String imagemPreviewDataUri
) { }
