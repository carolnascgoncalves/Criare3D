package br.com.criare3d.models.enums;
public enum StatusPedido {
 RASCUNHO("Rascunho"), ENVIADO("Enviado"), APROVADO("A produzir"), PRONTO("Pronto para entregar"), ENTREGUE("Entregue"), CANCELADO("Cancelado");
 private final String descricao;
 StatusPedido(String descricao) { this.descricao=descricao; }
 public String getDescricao() { return descricao; }
 public boolean isEditavel() { return this==RASCUNHO; }
}
