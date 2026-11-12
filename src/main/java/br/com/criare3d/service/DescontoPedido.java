package br.com.criare3d.service;
import java.math.*;
public record DescontoPedido(BigDecimal valor,BigDecimal percentual) {
 public static DescontoPedido interpretar(String texto,BigDecimal subtotal){
  if(texto==null||!texto.strip().matches("[0-9]{1,8}([.,][0-9]{1,2})?\\s*%?"))throw new IllegalArgumentException("Informe um desconto em reais (15 ou 15,50) ou porcentagem (15%).");
  String t=texto.strip();boolean porcentagem=t.endsWith("%");var n=new BigDecimal(t.replace("%","").strip().replace(',','.'));
  if(porcentagem&&n.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("O desconto percentual deve ficar entre 0% e 100%.");
  var valor=porcentagem?subtotal.multiply(n).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP):n.setScale(2);
  if(valor.compareTo(subtotal)>0)throw new IllegalArgumentException("O desconto não pode ultrapassar o valor das peças.");return new DescontoPedido(valor,porcentagem?n:null);
 }
}
