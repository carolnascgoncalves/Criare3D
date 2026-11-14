package br.com.criare3d.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formata quantidades do extrato: gramas viram kg quando passam de 1.000 e unidades não têm casas decimais. */
public final class FormatoQuantidade {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final BigDecimal MIL = new BigDecimal("1000");

    private FormatoQuantidade() {}

    private static String numero(BigDecimal valor, String padrao) {
        var formato = new DecimalFormat(padrao, DecimalFormatSymbols.getInstance(PT_BR));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor);
    }

    private static String sinal(BigDecimal valor, boolean comSinal) {
        if (valor.signum() < 0) return "-";
        return comSinal && valor.signum() > 0 ? "+" : "";
    }

    /** Versão curta para leitura rápida: "+5,42 kg", "-86 g", "-2 un.". */
    public static String resumido(BigDecimal valor, String medida, boolean comSinal) {
        if (valor == null) return "";
        var absoluto = valor.abs();
        var texto = sinal(valor, comSinal);
        if ("g".equals(medida)) {
            return absoluto.compareTo(MIL) >= 0
                    ? texto + numero(absoluto.divide(MIL), "#,##0.00") + " kg"
                    : texto + numero(absoluto, "#,##0.#") + " g";
        }
        return texto + numero(absoluto, "#,##0.###") + " " + medida;
    }

    /** Versão exata, para dica ao passar o mouse e para conferência: "+5.415,300 g". */
    public static String exato(BigDecimal valor, String medida) {
        if (valor == null) return "";
        var casas = "g".equals(medida) ? "#,##0.000###" : "#,##0.###";
        return sinal(valor, true) + numero(valor.abs(), casas) + " " + medida;
    }
}
