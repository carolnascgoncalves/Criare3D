package br.com.criare3d.dto;

import java.util.List;
import java.util.UUID;

public record PecaArquivoResumo(
        UUID pecaId,
        String nomeArquivo,
        String modeloImpressora,
        String fatiador,
        int numeroPlacas,
        List<PecaMaterialResumo> materiais,
        List<PlacaResumo> placas
) {
    public boolean temMiniatura() {
        return placas != null && placas.stream().anyMatch(PlacaResumo::temImagem);
    }

    public String urlMiniatura() {
        if (placas == null) return null;
        return placas.stream().filter(PlacaResumo::temImagem).findFirst()
                .map(PlacaResumo::urlImagem).orElse(null);
    }

    public record PlacaResumo(int numero, String urlImagem, Double tempoSegundos,
                              Double pesoGramas, List<PecaMaterialResumo> materiais) {
        public boolean temImagem() { return urlImagem != null && !urlImagem.isBlank(); }
    }
}
