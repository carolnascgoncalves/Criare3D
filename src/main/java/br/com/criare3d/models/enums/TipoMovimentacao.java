package br.com.criare3d.models.enums;

/** Tipos de movimentação do extrato, com o texto e o ícone exibidos na tela. */
public enum TipoMovimentacao {
    SALDO_INICIAL("Saldo de abertura", "bi-flag"),
    COMPRA("Compra", "bi-bag-plus"),
    AJUSTE("Ajuste", "bi-sliders"),
    PRODUCAO("Produção", "bi-printer"),
    DESFAZER_PRODUCAO("Produção desfeita", "bi-arrow-counterclockwise"),
    PERDA("Perda", "bi-exclamation-triangle"),
    CANCELAR_PERDA("Perda cancelada", "bi-arrow-counterclockwise"),
    VENDA_3D("Venda", "bi-bag-check"),
    CANCELAR_VENDA_3D("Venda cancelada", "bi-arrow-counterclockwise"),
    VENDA_PRODUTO("Venda de produto", "bi-shop"),
    CANCELAR_VENDA_PRODUTO("Venda cancelada", "bi-arrow-counterclockwise"),
    OUTRO("Movimentação", "bi-arrow-left-right");

    private final String rotulo;
    private final String icone;

    TipoMovimentacao(String rotulo, String icone) {
        this.rotulo = rotulo;
        this.icone = icone;
    }

    public String getRotulo() { return rotulo; }
    public String getIcone() { return icone; }

    /** Tipos desconhecidos (gravados por versões futuras) caem em OUTRO em vez de quebrar a tela. */
    public static TipoMovimentacao de(String nome) {
        if (nome == null) return OUTRO;
        try { return valueOf(nome); } catch (IllegalArgumentException e) { return OUTRO; }
    }

    public static boolean valido(String nome) {
        if (nome == null || nome.isBlank()) return false;
        try { return valueOf(nome) != OUTRO; } catch (IllegalArgumentException e) { return false; }
    }
}
