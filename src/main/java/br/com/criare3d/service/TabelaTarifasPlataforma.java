package br.com.criare3d.service;

import java.math.BigDecimal;

/** Reúne os parâmetros das estimativas de tarifas usadas pelos dois modos de cálculo. */
final class TabelaTarifasPlataforma {
    private static final ParametrosTarifas PARAMETROS = ParametrosTarifas.carregar();
    static final BigDecimal SHOPEE_COMISSAO_ATE_79_99 = PARAMETROS.decimal("SHOPEE_COMISSAO_ATE_79_99");
    static final BigDecimal SHOPEE_COMISSAO_ACIMA_79_99 = PARAMETROS.decimal("SHOPEE_COMISSAO_ACIMA_79_99");
    static final BigDecimal SHOPEE_TAXA_CPF = PARAMETROS.decimal("SHOPEE_TAXA_CPF");
    static final BigDecimal SHOPEE_FIXO_ATE_79_99 = PARAMETROS.decimal("SHOPEE_FIXO_ATE_79_99");
    static final BigDecimal SHOPEE_FIXO_80_A_99_99 = PARAMETROS.decimal("SHOPEE_FIXO_80_A_99_99");
    static final BigDecimal SHOPEE_FIXO_100_A_199_99 = PARAMETROS.decimal("SHOPEE_FIXO_100_A_199_99");
    static final BigDecimal SHOPEE_FIXO_A_PARTIR_200 = PARAMETROS.decimal("SHOPEE_FIXO_A_PARTIR_200");
    static final BigDecimal SHOPEE_LIMITE_80 = PARAMETROS.decimal("SHOPEE_LIMITE_80");
    static final BigDecimal SHOPEE_LIMITE_100 = PARAMETROS.decimal("SHOPEE_LIMITE_100");
    static final BigDecimal SHOPEE_LIMITE_200 = PARAMETROS.decimal("SHOPEE_LIMITE_200");
    static final BigDecimal SHOPEE_PRECO_MINIMO = PARAMETROS.decimal("SHOPEE_PRECO_MINIMO");
    static final BigDecimal SHOPEE_COMISSAO_EQUIVALENTE_PRECO_BAIXO = PARAMETROS.decimal("SHOPEE_COMISSAO_EQUIVALENTE_PRECO_BAIXO");
    static final BigDecimal SHOPEE_INICIO_FAIXA_80 = PARAMETROS.decimal("SHOPEE_INICIO_FAIXA_80");
    static final BigDecimal SHOPEE_INICIO_FAIXA_100 = PARAMETROS.decimal("SHOPEE_INICIO_FAIXA_100");
    static final BigDecimal SHOPEE_INICIO_FAIXA_200 = PARAMETROS.decimal("SHOPEE_INICIO_FAIXA_200");

    static final BigDecimal TIKTOK_LIMITE_PRIMEIRA_FAIXA = PARAMETROS.decimal("TIKTOK_LIMITE_PRIMEIRA_FAIXA");
    static final BigDecimal TIKTOK_COMISSAO_PRIMEIRA_FAIXA = PARAMETROS.decimal("TIKTOK_COMISSAO_PRIMEIRA_FAIXA");
    static final BigDecimal TIKTOK_COMISSAO_DEMAIS_FAIXAS = PARAMETROS.decimal("TIKTOK_COMISSAO_DEMAIS_FAIXAS");
    static final BigDecimal TIKTOK_TAXA_FIXA_PRIMEIRA_FAIXA = PARAMETROS.decimal("TIKTOK_TAXA_FIXA_PRIMEIRA_FAIXA");
    static final BigDecimal TIKTOK_TAXA_FIXA_DEMAIS_FAIXAS = PARAMETROS.decimal("TIKTOK_TAXA_FIXA_DEMAIS_FAIXAS");
    static final BigDecimal TIKTOK_PERCENTUAL_FRETE_GRATIS = PARAMETROS.decimal("TIKTOK_PERCENTUAL_FRETE_GRATIS");
    static final BigDecimal TIKTOK_TETO_TAXA_FRETE = PARAMETROS.decimal("TIKTOK_TETO_TAXA_FRETE");

    static final BigDecimal ML_CLASSICO_COMISSAO_PADRAO = PARAMETROS.decimal("ML_CLASSICO_COMISSAO_PADRAO");
    static final BigDecimal ML_PREMIUM_COMISSAO_PADRAO = PARAMETROS.decimal("ML_PREMIUM_COMISSAO_PADRAO");
    static final BigDecimal ML_CLASSICO_COMISSAO_MINIMA = PARAMETROS.decimal("ML_CLASSICO_COMISSAO_MINIMA");
    static final BigDecimal ML_PREMIUM_COMISSAO_MINIMA = PARAMETROS.decimal("ML_PREMIUM_COMISSAO_MINIMA");
    static final BigDecimal ML_CLASSICO_COMISSAO_MAXIMA = PARAMETROS.decimal("ML_CLASSICO_COMISSAO_MAXIMA");
    static final BigDecimal ML_PREMIUM_COMISSAO_MAXIMA = PARAMETROS.decimal("ML_PREMIUM_COMISSAO_MAXIMA");

    static final BigDecimal AMAZON_COMISSAO_CASA = PARAMETROS.decimal("AMAZON_COMISSAO_CASA");
    static final BigDecimal AMAZON_COMISSAO_MINIMA = PARAMETROS.decimal("AMAZON_COMISSAO_MINIMA");

    private static final BigDecimal[] LIMITES_PRECO_ML = PARAMETROS.lista("ML_LIMITES_PRECO", 4);
    private static final BigDecimal[][] FRETES_ML = {
            PARAMETROS.lista("ML_FRETES_ATE_300G", 4),
            PARAMETROS.lista("ML_FRETES_DE_300G_500G", 4),
            PARAMETROS.lista("ML_FRETES_DE_500G_1KG", 4),
            PARAMETROS.lista("ML_FRETES_DE_1KG_1_5KG", 4),
            PARAMETROS.lista("ML_FRETES_DE_1_5KG_2KG", 4),
            PARAMETROS.lista("ML_FRETES_DE_2KG_3KG", 4),
            PARAMETROS.lista("ML_FRETES_DE_3KG_4KG", 4),
            PARAMETROS.lista("ML_FRETES_DE_4KG_5KG", 4)
    };

    private TabelaTarifasPlataforma() { }

    static void validar() { PARAMETROS.validar(); }

    static BigDecimal freteMercadoLivre(String faixaPeso, BigDecimal preco) {
        int coluna = 0;
        while (coluna < LIMITES_PRECO_ML.length && preco.compareTo(LIMITES_PRECO_ML[coluna]) > 0) {
            coluna++;
        }
        if (coluna == LIMITES_PRECO_ML.length) {
            throw new IllegalArgumentException(
                    "O preço calculado passou de R$ " + LIMITES_PRECO_ML[LIMITES_PRECO_ML.length - 1].toPlainString().replace('.', ',') + ". Informe o frete manualmente.");
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
