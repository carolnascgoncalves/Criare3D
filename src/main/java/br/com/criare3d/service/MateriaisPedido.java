package br.com.criare3d.service;

import br.com.criare3d.models.*;
import java.math.*;
import java.util.*;

/** Previsão de compras, sem reservar ou movimentar filamento. */
public final class MateriaisPedido {
 private MateriaisPedido() {}
 public record Material(UUID id,String nome,String cor,BigDecimal necessario,BigDecimal estoque,
                        BigDecimal falta,Integer rolos,BigDecimal reposicao) {}
 public record Resumo(List<Material> materiais,BigDecimal reposicao,boolean precoIndisponivel) {
  public long getFaltantes(){return materiais.stream().filter(m->m.falta().signum()>0).count();}
 }
 public static Resumo resumir(List<CalculoHistorico> lotes) {
  Map<UUID,BigDecimal> consumo=new LinkedHashMap<>();Map<UUID,Filamento> filamentos=new LinkedHashMap<>();
  for(var lote:lotes) {
   var itens=lote.getItensFilamento();
   if(itens.isEmpty()) {
    if(lote.getFilamento()==null||lote.getQuantidadeFilamentoGramas()==null)throw new IllegalArgumentException("Não foi possível identificar os materiais deste lote. Revise seu cálculo.");
    itens=List.of(ItemCalculoHistorico.builder().filamento(lote.getFilamento()).quantidadeFilamentoGramas(lote.getQuantidadeFilamentoGramas()).build());
   }
   for(var item:itens) {
   var f=item.getFilamento();if(f==null)throw new IllegalArgumentException("Um filamento não está mais disponível. Revise os materiais do cálculo.");
   filamentos.put(f.getId(),f);
   var gramas=item.getQuantidadeFilamentoGramas().add(item.getPurgaGramas()==null?BigDecimal.ZERO:item.getPurgaGramas()).multiply(BigDecimal.valueOf(lote.getUnidades()));
   consumo.merge(f.getId(),gramas,BigDecimal::add);
  }
  }
  List<Material> resultado=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;boolean semPreco=false;
  for(var e:consumo.entrySet()) {
   var f=filamentos.get(e.getKey());var estoque=f.getEstoqueGramas()==null?BigDecimal.ZERO:f.getEstoqueGramas();
   var falta=e.getValue().subtract(estoque).max(BigDecimal.ZERO);Integer rolos=0;BigDecimal custo=BigDecimal.ZERO;
   if(falta.signum()>0) {
    if(f.getPesoRoloGramas()==null||f.getPesoRoloGramas().signum()<=0||f.getPrecoRolo()==null){rolos=null;custo=null;semPreco=true;}
    else {rolos=falta.divide(f.getPesoRoloGramas(),0,RoundingMode.CEILING).intValueExact();custo=f.getPrecoRolo().multiply(BigDecimal.valueOf(rolos)).setScale(2,RoundingMode.HALF_UP);total=total.add(custo);}
   }
   resultado.add(new Material(f.getId(),f.getNome(),f.getCor(),e.getValue(),estoque,falta,rolos,custo));
  }
  return new Resumo(List.copyOf(resultado),total,semPreco);
 }
}
