package br.com.criare3d.dto;
import br.com.criare3d.models.CalculoHistorico;
import java.math.BigDecimal;
import java.util.*;

/** Apenas valores financeiros e IDs; sem entidades ou dados de sessão no JSON do pedido. */
public record CustosPreservados(BigDecimal tarifa,BigDecimal embalagem,String nomeEmbalagem,
 Boolean maoObra,BigDecimal valorHora,Boolean depreciacao,Boolean falhas,BigDecimal percentualFalhas,
 BigDecimal purga,BigDecimal manutencao,BigDecimal potencia,Integer aquecimento,BigDecimal compra,
 BigDecimal vidaUtil,BigDecimal depreciacaoHora,List<Material> materiais,List<Extra> extras) {
 public record Material(UUID id,BigDecimal preco,BigDecimal peso) {}
 public record Extra(UUID id,String descricao,BigDecimal valor) {}
 public static CustosPreservados de(CalculoHistorico h) {
  var materiais=h.getItensFilamento().stream().filter(i->i.getFilamento()!=null).map(i->new Material(i.getFilamento().getId(),i.getPrecoRoloFilamento(),i.getPesoRoloGramas())).toList();
  if(materiais.isEmpty()&&h.getFilamento()!=null)materiais=List.of(new Material(h.getFilamento().getId(),h.getPrecoRoloFilamento(),h.getPesoRoloGramas()));
  var extras=h.getRateiosCustos().stream().map(i->new Extra(i.getCustoLoja().getId(),i.getDescricao(),i.getValorUnitario())).toList();
  return new CustosPreservados(h.getTarifaEnergiaKwh(),h.getCustoEmbalagem(),h.getNomeEmbalagem(),h.getMaoObraAtivada(),h.getValorMaoObraHora(),h.getDepreciacaoAtivada(),h.getTaxaFalhaAtivada(),h.getPercentualFalha(),h.getPurgaGramasPorImpressao(),h.getManutencaoHoraImpressora(),h.getPotenciaWatts(),h.getTempoAquecimentoMinutos(),h.getValorCompraImpressora(),h.getVidaUtilImpressoraHoras(),h.getDepreciacaoHoraImpressora(),materiais,extras);
 }
}
