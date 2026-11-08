package br.com.criare3d.service;

/** Regras puras para distribuir unidades entre impressões. */
public final class ModeloImpressao {
    private ModeloImpressao() {}

    public static int quantidadeImpressoes(int unidades, int pecasPorImpressao) {
        if (unidades < 1 || pecasPorImpressao < 1) {
            throw new IllegalArgumentException("Unidades e capacidade devem ser maiores que zero.");
        }
        return (unidades + pecasPorImpressao - 1) / pecasPorImpressao;
    }

    public static long minutosTotais(long minutosPorImpressao, int tempoAquecimentoMinutos,
                                     int quantidadeImpressoes) {
        if (minutosPorImpressao < 0 || tempoAquecimentoMinutos < 0 || quantidadeImpressoes < 1) {
            throw new IllegalArgumentException("Informe tempos e quantidade de impressões válidos.");
        }
        return Math.multiplyExact(Math.addExact(minutosPorImpressao, tempoAquecimentoMinutos),
                (long) quantidadeImpressoes);
    }
}
