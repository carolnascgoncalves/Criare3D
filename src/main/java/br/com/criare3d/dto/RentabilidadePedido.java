package br.com.criare3d.dto;
import java.math.*;
/** Sinalização indicativa; não altera preços nem bloqueia a proposta. */
public record RentabilidadePedido(String estado,String titulo,BigDecimal lucro,BigDecimal margem) {
 public static RentabilidadePedido avaliar(BigDecimal receita,BigDecimal custo,BigDecimal taxas){
  BigDecimal lucro=receita.subtract(custo).subtract(taxas);
  BigDecimal margem=receita.signum()>0?lucro.multiply(BigDecimal.valueOf(100)).divide(receita,8,RoundingMode.HALF_UP):BigDecimal.ZERO;
  if(lucro.signum()<=0)return new RentabilidadePedido("prejuizo","Não recomendado neste preço",lucro,margem);
  if(margem.compareTo(BigDecimal.valueOf(15))<0)return new RentabilidadePedido("baixa","Margem apertada: revise o preço",lucro,margem);
  return new RentabilidadePedido("boa","Boa margem nesta estimativa",lucro,margem);
 }
}
