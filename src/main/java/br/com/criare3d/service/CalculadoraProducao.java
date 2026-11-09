package br.com.criare3d.service;

import java.math.BigDecimal;

/** Calcula a composição de custos unitários a partir dos custos do lote. */
public final class CalculadoraProducao {
    private CalculadoraProducao() {}

    /** Centraliza as parcelas variáveis do lote; o serviço fica responsável por buscar e validar os dados. */
    public static CustosOperacionaisLote calcularCustosOperacionaisLote(
            BigDecimal potenciaWatts, long minutosMaquina, BigDecimal tarifaEnergiaKwh,
            BigDecimal gramasPurgaPorImpressao, int impressoes,
            BigDecimal custoFilamentoLote, BigDecimal gramasFilamentoUnidade,
            boolean maoDeObraAtivada, BigDecimal valorMaoDeObraHora, long minutosTrabalho,
            BigDecimal manutencaoPorHora, boolean depreciacaoAtivada,
            BigDecimal valorCompraImpressora, BigDecimal vidaUtilHoras) {
        BigDecimal energia = energiaLote(potenciaWatts, minutosMaquina, tarifaEnergiaKwh);
        BigDecimal gramasPurga = gramasPurgaLote(gramasPurgaPorImpressao, impressoes);
        BigDecimal custoPurga = custoPurgaLote(gramasPurga, custoFilamentoLote, gramasFilamentoUnidade);
        BigDecimal maoDeObra = maoDeObraAtivada
                ? CalculoFinanceiro.maoDeObra(valorMaoDeObraHora, minutosTrabalho)
                : BigDecimal.ZERO.setScale(8);
        BigDecimal manutencao = manutencaoLote(manutencaoPorHora, minutosMaquina);
        BigDecimal depreciacao = depreciacaoAtivada
                ? depreciacaoLote(valorCompraImpressora, vidaUtilHoras, minutosMaquina)
                : BigDecimal.ZERO.setScale(8);
        return new CustosOperacionaisLote(energia, gramasPurga, custoPurga, maoDeObra,
                manutencao, depreciacao);
    }

    public record CustosOperacionaisLote(BigDecimal energia, BigDecimal gramasPurga,
                                          BigDecimal custoPurga, BigDecimal maoDeObra,
                                          BigDecimal manutencao, BigDecimal depreciacao) {}

    public static BigDecimal energiaLote(BigDecimal potenciaWatts, long minutos,
                                         BigDecimal tarifaEnergiaKwh) {
        return CalculoFinanceiro.energia(potenciaWatts, minutos, tarifaEnergiaKwh);
    }

    public static BigDecimal manutencaoLote(BigDecimal custoPorHora, long minutos) {
        return CalculoFinanceiro.manutencao(custoPorHora, minutos);
    }

    public static BigDecimal depreciacaoLote(BigDecimal valorCompra, BigDecimal vidaUtilHoras,
                                             long minutos) {
        return CalculoFinanceiro.depreciacao(valorCompra, vidaUtilHoras, minutos);
    }

    public static BigDecimal gramasPurgaLote(BigDecimal gramasPorImpressao, int impressoes) {
        return CalculoFinanceiro.precisao(gramasPorImpressao.multiply(BigDecimal.valueOf(impressoes)));
    }

    public static BigDecimal custoPurgaLote(BigDecimal gramasPurgaLote,
                                            BigDecimal custoFilamentoLote,
                                            BigDecimal gramasFilamentoPorUnidade) {
        BigDecimal gramasTotais = gramasFilamentoPorUnidade;
        if (gramasTotais.signum() <= 0) return BigDecimal.ZERO.setScale(8);
        return gramasPurgaLote.multiply(custoFilamentoLote)
                .divide(gramasTotais, 8, java.math.RoundingMode.HALF_UP);
    }

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
