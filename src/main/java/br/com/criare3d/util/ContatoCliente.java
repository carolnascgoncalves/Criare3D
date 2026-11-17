package br.com.criare3d.util;

import java.util.Locale;

public final class ContatoCliente {
    private ContatoCliente() {
    }

    public static String digitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    public static String telefoneNormalizado(String telefone) {
        var numero = digitos(telefone);
        return numero.length() > 11 && numero.startsWith("55") ? numero.substring(2) : numero;
    }

    public static String formatarTelefone(String telefone) {
        var d = telefoneNormalizado(telefone);
        if (d.length() == 11) return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        if (d.length() == 10) return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        return telefone == null ? "" : telefone;
    }

    public static String linkWhatsapp(String telefone) {
        var d = digitos(telefone);
        if (d.length() == 10 || d.length() == 11) return "https://wa.me/55" + d;
        if ((d.length() == 12 || d.length() == 13) && d.startsWith("55")) return "https://wa.me/" + d;
        return null;
    }

    public static String iniciais(String nome) {
        var partes = nome == null ? new String[0] : nome.strip().split("\\s+");
        if (partes.length == 0 || partes[0].isEmpty()) return "?";
        var primeira = partes[0].substring(0, 1);
        return (partes.length > 1 ? primeira + partes[partes.length - 1].substring(0, 1) : primeira)
            .toUpperCase(Locale.ROOT);
    }
}
