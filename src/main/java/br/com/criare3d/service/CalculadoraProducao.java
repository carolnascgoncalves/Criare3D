package br.com.criare3d.service;

import java.math.BigDecimal;

/** Calcula a composição de custos unitários a partir dos custos do lote. */
public final class CalculadoraProducao {
    private CalculadoraProducao() {}

    public static Resultado calcular(Entrada entrada) {
        int unidades = entrada.unidades();
        if (unidades < 1) throw new IllegalArgumentException("A quantidade de unidades deve ser positiva.");

        BigDecimal custoPurga = CalculoFinanceiro.ratear(entrada.custoPurgaLote(), unidades);
        BigDecimal custoEnergia = CalculoFinanceiro.ratear(entrada.custoEnergiaLote(), unidades);
        BigDecimal custoMaoObra = CalculoFinanceiro.ratear(entrada.custoMaoObraLote(), unidades);
        BigDecimal custoDepreciacao = CalculoFinanceiro.ratear(entrada.custoDepreciacaoLote(), unidades);
        BigDecimal custoManutencao = CalculoFinanceiro.ratear(entrada.custoManutencaoLote(), unidades);
        BigDecimal custoFalha = entrada.taxaFalhaAtivada()
                ? CalculoFinanceiro.falhas(entrada.custoFilamentoUnitario().add(custoPurga),
                    custoEnergia, entrada.percentualFalha())
                : BigDecimal.ZERO.setScale(8);
        BigDecimal custoTotal = CalculoFinanceiro.precisao(
                entrada.custoFilamentoUnitario().add(custoPurga).add(custoEnergia)
                        .add(entrada.custoEmbalagemUnitario()).add(custoMaoObra)
                        .add(custoDepreciacao).add(custoManutencao).add(custoFalha));
        BigDecimal precoSugerido = CalculoFinanceiro.markup(custoTotal, entrada.markupPercentual());

        return new Resultado(custoPurga, custoEnergia, custoMaoObra, custoDepreciacao,
                custoManutencao, custoFalha, custoTotal, precoSugerido);
    }

    public record Entrada(int unidades, BigDecimal custoFilamentoUnitario,
                          BigDecimal custoPurgaLote, BigDecimal custoEnergiaLote,
                          BigDecimal custoEmbalagemUnitario, BigDecimal custoMaoObraLote,
                          BigDecimal custoDepreciacaoLote, BigDecimal custoManutencaoLote,
                          boolean taxaFalhaAtivada, BigDecimal percentualFalha,
                          BigDecimal markupPercentual) {}

    public record Resultado(BigDecimal custoPurga, BigDecimal custoEnergia,
                            BigDecimal custoMaoObra, BigDecimal custoDepreciacao,
                            BigDecimal custoManutencao, BigDecimal custoFalha,
                            BigDecimal custoTotal, BigDecimal precoSugerido) {}
}
