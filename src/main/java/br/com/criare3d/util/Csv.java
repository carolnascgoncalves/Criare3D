package br.com.criare3d.util;

import java.math.BigDecimal;

public final class Csv {
    private Csv() {}

    public static String campo(String valor) {
        if (valor == null) return "\"\"";
        String seguro = valor.replace('\r', ' ').replace('\n', ' ');
        String inicio = seguro.stripLeading();
        if (!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0) seguro = "'" + seguro;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    public static String numero(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString().replace('.', ',');
    }
}
