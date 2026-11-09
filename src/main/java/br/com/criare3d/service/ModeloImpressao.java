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

    /**
     * O tempo informado é por unidade. Cada unidade soma seu tempo de máquina;
     * o aquecimento é cobrado uma vez por impressão, inclusive a parcial.
     */
    public static long minutosTotais(long minutosImpressaoCheia, int aquecimentoPorImpressao,
                                     int unidades, int pecasPorImpressao) {
        if (minutosImpressaoCheia < 0 || aquecimentoPorImpressao < 0
                || unidades < 1 || pecasPorImpressao < 1) {
            throw new IllegalArgumentException("Informe tempos, unidades e capacidade válidos.");
        }

        int impressoes = quantidadeImpressoes(unidades, pecasPorImpressao);
        long minutosUnidades = Math.multiplyExact(minutosImpressaoCheia, (long) unidades);
        long minutosAquecimento = Math.multiplyExact((long) aquecimentoPorImpressao, impressoes);
        return Math.addExact(minutosUnidades, minutosAquecimento);
    }
}
