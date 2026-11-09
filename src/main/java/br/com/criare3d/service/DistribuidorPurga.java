package br.com.criare3d.service;

import br.com.criare3d.models.ItemCalculoHistorico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Distribui a purga de cada impressão entre os filamentos usados no lote. */
public final class DistribuidorPurga {
    private DistribuidorPurga() {}

    public static void distribuir(BigDecimal purgaPorUnidade,
                                  BigDecimal gramasBasePorUnidade,
                                  List<ItemCalculoHistorico> itens) {
        if (purgaPorUnidade == null || purgaPorUnidade.signum() < 0
                || gramasBasePorUnidade == null || gramasBasePorUnidade.signum() < 0
                || itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Informe filamentos e quantidades válidas para distribuir a purga.");
        }
        if (gramasBasePorUnidade.signum() == 0) {
            throw new IllegalArgumentException("A quantidade total de filamento deve ser maior que zero.");
        }

        BigDecimal alocada = BigDecimal.ZERO;
        for (int i = 0; i < itens.size(); i++) {
            ItemCalculoHistorico item = itens.get(i);
            BigDecimal parcela = i == itens.size() - 1
                    ? CalculoFinanceiro.precisao(purgaPorUnidade.subtract(alocada))
                    : CalculoFinanceiro.precisao(purgaPorUnidade
                        .multiply(item.getQuantidadeFilamentoGramas())
                        .divide(gramasBasePorUnidade, 8, RoundingMode.HALF_UP));
            item.setPurgaGramas(parcela);
            alocada = alocada.add(parcela);
        }
    }
}
