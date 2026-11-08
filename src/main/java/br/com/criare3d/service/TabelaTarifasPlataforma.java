package br.com.criare3d.service;

import java.math.BigDecimal;

/** Reúne os parâmetros das estimativas de tarifas usadas pelos dois modos de cálculo. */
final class TabelaTarifasPlataforma {
    static final BigDecimal SHOPEE_COMISSAO_ATE_79_99 = new BigDecimal("20.00");
    static final BigDecimal SHOPEE_COMISSAO_ACIMA_79_99 = new BigDecimal("14.00");
    static final BigDecimal SHOPEE_TAXA_CPF = new BigDecimal("3.00");
    static final BigDecimal SHOPEE_FIXO_ATE_79_99 = new BigDecimal("4.50");
    static final BigDecimal SHOPEE_FIXO_80_A_99_99 = new BigDecimal("16.00");
    static final BigDecimal SHOPEE_FIXO_100_A_199_99 = new BigDecimal("20.00");
    static final BigDecimal SHOPEE_FIXO_A_PARTIR_200 = new BigDecimal("26.00");
    static final BigDecimal SHOPEE_LIMITE_80 = new BigDecimal("79.99");
    static final BigDecimal SHOPEE_LIMITE_100 = new BigDecimal("99.99");
    static final BigDecimal SHOPEE_LIMITE_200 = new BigDecimal("199.99");
    static final BigDecimal SHOPEE_PRECO_MINIMO = new BigDecimal("9.00");
    static final BigDecimal SHOPEE_COMISSAO_EQUIVALENTE_PRECO_BAIXO = new BigDecimal("50.00");
    static final BigDecimal SHOPEE_INICIO_FAIXA_80 = new BigDecimal("80.00");
    static final BigDecimal SHOPEE_INICIO_FAIXA_100 = new BigDecimal("100.00");
    static final BigDecimal SHOPEE_INICIO_FAIXA_200 = new BigDecimal("200.00");

    static final BigDecimal TIKTOK_LIMITE_PRIMEIRA_FAIXA = new BigDecimal("50.00");
    static final BigDecimal TIKTOK_COMISSAO_PRIMEIRA_FAIXA = new BigDecimal("10.00");
    static final BigDecimal TIKTOK_COMISSAO_DEMAIS_FAIXAS = new BigDecimal("6.00");
    static final BigDecimal TIKTOK_TAXA_FIXA_PRIMEIRA_FAIXA = new BigDecimal("4.00");
    static final BigDecimal TIKTOK_TAXA_FIXA_DEMAIS_FAIXAS = new BigDecimal("6.00");
    static final BigDecimal TIKTOK_PERCENTUAL_FRETE_GRATIS = new BigDecimal("6.00");
    static final BigDecimal TIKTOK_TETO_TAXA_FRETE = new BigDecimal("50.00");

    static final BigDecimal ML_CLASSICO_COMISSAO_PADRAO = new BigDecimal("14.00");
    static final BigDecimal ML_PREMIUM_COMISSAO_PADRAO = new BigDecimal("19.00");
    static final BigDecimal ML_CLASSICO_COMISSAO_MINIMA = new BigDecimal("10.00");
    static final BigDecimal ML_PREMIUM_COMISSAO_MINIMA = new BigDecimal("15.00");
    static final BigDecimal ML_CLASSICO_COMISSAO_MAXIMA = new BigDecimal("14.00");
    static final BigDecimal ML_PREMIUM_COMISSAO_MAXIMA = new BigDecimal("19.00");

    private static final BigDecimal[] LIMITES_PRECO_ML = {
            new BigDecimal("48.99"), new BigDecimal("78.99"),
            new BigDecimal("99.99"), new BigDecimal("119.99")
    };
    private static final BigDecimal[][] FRETES_ML = {
            {new BigDecimal("6.85"), new BigDecimal("8.15"), new BigDecimal("12.95"), new BigDecimal("14.95")},
            {new BigDecimal("6.95"), new BigDecimal("8.25"), new BigDecimal("13.85"), new BigDecimal("16.15")},
            {new BigDecimal("7.15"), new BigDecimal("8.45"), new BigDecimal("14.45"), new BigDecimal("16.85")},
            {new BigDecimal("7.35"), new BigDecimal("8.65"), new BigDecimal("14.75"), new BigDecimal("17.15")},
            {new BigDecimal("7.45"), new BigDecimal("8.75"), new BigDecimal("15.05"), new BigDecimal("17.65")},
            {new BigDecimal("8.65"), new BigDecimal("9.15"), new BigDecimal("16.45"), new BigDecimal("19.15")},
            {new BigDecimal("8.75"), new BigDecimal("9.75"), new BigDecimal("17.85"), new BigDecimal("20.75")},
            {new BigDecimal("8.85"), new BigDecimal("10.25"), new BigDecimal("19.75"), new BigDecimal("22.85")}
    };

    private TabelaTarifasPlataforma() { }

    static BigDecimal freteMercadoLivre(String faixaPeso, BigDecimal preco) {
        int coluna = 0;
        while (coluna < LIMITES_PRECO_ML.length && preco.compareTo(LIMITES_PRECO_ML[coluna]) > 0) {
            coluna++;
        }
        if (coluna == LIMITES_PRECO_ML.length) {
            throw new IllegalArgumentException(
                    "O preço calculado passou de R$ 119,99. Informe o frete manualmente.");
        }

        int linha = switch (faixaPeso) {
            case "ATE_300G" -> 0;
            case "DE_300G_500G" -> 1;
            case "DE_500G_1KG" -> 2;
            case "DE_1KG_1_5KG" -> 3;
            case "DE_1_5KG_2KG" -> 4;
            case "DE_2KG_3KG" -> 5;
            case "DE_3KG_4KG" -> 6;
            case "DE_4KG_5KG" -> 7;
            default -> throw new IllegalArgumentException(
                    "Faixa de peso do Mercado Livre ainda não configurada.");
        };
        return FRETES_ML[linha][coluna];
    }
}
