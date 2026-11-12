package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class MateriaisPedidoTest {
 private Filamento filamento(String estoque){return Filamento.builder().id(UUID.randomUUID()).nome("PLA").cor("Azul").estoqueGramas(new BigDecimal(estoque)).pesoRoloGramas(new BigDecimal("1000")).precoRolo(new BigDecimal("80")).build();}
 private CalculoHistorico lote(Filamento f,int unidades,String peso,String purga){var h=new CalculoHistorico();h.setUnidades(unidades);h.getItensFilamento().add(ItemCalculoHistorico.builder().filamento(f).quantidadeFilamentoGramas(new BigDecimal(peso)).purgaGramas(new BigDecimal(purga)).build());return h;}
 @Test void somaMesmoFilamentoEntrePecasIncluindoPurgaECompraRolosInteiros(){
  var f=filamento("100");var r=MateriaisPedido.resumir(List.of(lote(f,2,"300","10"),lote(f,3,"200","0")));
  var m=r.materiais().getFirst();assertThat(m.necessario()).isEqualByComparingTo("1220");assertThat(m.falta()).isEqualByComparingTo("1120");assertThat(m.rolos()).isEqualTo(2);assertThat(r.reposicao()).isEqualByComparingTo("160");assertThat(f.getEstoqueGramas()).isEqualByComparingTo("100");
 }
 @Test void estoqueSuficienteNaoSugereCompraESemPrecoNaoInventaZero(){
  var f=filamento("500");assertThat(MateriaisPedido.resumir(List.of(lote(f,1,"100","0"))).reposicao()).isZero();
  f.setPrecoRolo(null);var r=MateriaisPedido.resumir(List.of(lote(f,1,"600","0")));assertThat(r.precoIndisponivel()).isTrue();assertThat(r.materiais().getFirst().reposicao()).isNull();
 }
 @Test void pecasDoEstoqueEProducaoConcluidaNaoConsomemNovamente(){
  var calculos=mock(CalculoService.class);var entradas=mock(EntradaPedidoService.class);
  var historicos=mock(CalculoHistoricoRepository.class);
  var service=new OrcamentoPedidoService(mock(PedidoOrcamentoRepository.class),mock(ItemPedidoRepository.class),historicos,calculos,entradas,Clock.systemUTC(),mock(FotoPedidoService.class),mock(PlanejamentoPedidoService.class));
  var p=new PedidoOrcamento();var pronta=new ItemPedido();pronta.setQuantidade(4);pronta.setUsarEstoque(4);p.getItens().add(pronta);
  var f=filamento("10");var h=lote(f,2,"25","1");var pendente=new ItemPedido();pendente.setQuantidade(2);h.setId(UUID.randomUUID());pendente.setLoteProducao(h);p.getItens().add(pendente);when(historicos.buscarPorIdComItens(h.getId())).thenReturn(Optional.of(h));
  assertThat(service.materiais(p).materiais().getFirst().necessario()).isEqualByComparingTo("52");
  h.setProduzido(true);assertThat(service.materiais(p).materiais()).isEmpty();verifyNoInteractions(calculos,entradas);
 }
 @Test void recarregaLoteDesconectadoAntesDeConsultarMateriais(){
  var historicos=mock(CalculoHistoricoRepository.class);var calculos=mock(CalculoService.class);
  var service=new OrcamentoPedidoService(mock(PedidoOrcamentoRepository.class),mock(ItemPedidoRepository.class),historicos,calculos,new EntradaPedidoService(),Clock.systemUTC(),mock(FotoPedidoService.class),mock(PlanejamentoPedidoService.class));
  var desconectado=mock(CalculoHistorico.class);var id=UUID.randomUUID();when(desconectado.getId()).thenReturn(id);
  when(desconectado.getItensFilamento()).thenThrow(new org.hibernate.LazyInitializationException("no session"));
  var atual=lote(filamento("100"),2,"60","1");when(historicos.buscarPorIdComItens(id)).thenReturn(Optional.of(atual));
  var p=new PedidoOrcamento();var i=new ItemPedido();i.setQuantidade(2);i.setLoteProducao(desconectado);p.getItens().add(i);
  var r=service.materiais(p);assertThat(r.materiais().getFirst().necessario()).isEqualByComparingTo("122");assertThat(r.getFaltantes()).isEqualTo(1);
  verify(desconectado,never()).getItensFilamento();verifyNoInteractions(calculos);
 }

}
