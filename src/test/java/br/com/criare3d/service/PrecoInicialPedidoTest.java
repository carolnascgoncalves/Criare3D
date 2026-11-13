package br.com.criare3d.service;
import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class PrecoInicialPedidoTest {
 @Test void preservaLucroDoPrecoPersonalizadoMesmoComMarkupZero() {
  var pedidos=mock(PedidoOrcamentoRepository.class);var itens=mock(ItemPedidoRepository.class);var historicos=mock(CalculoHistoricoRepository.class);var calculos=mock(CalculoService.class);
  var service=new OrcamentoPedidoService(pedidos,itens,historicos,calculos,new EntradaPedidoService(),Clock.systemUTC(),mock(FotoPedidoService.class),mock(PlanejamentoPedidoService.class));
  var p=new PedidoOrcamento();p.setId(UUID.randomUUID());when(pedidos.bloquear(p.getId())).thenReturn(Optional.of(p));
  var h=new CalculoHistorico();h.setId(UUID.randomUUID());h.setPeca(Peca.builder().id(UUID.randomUUID()).build());h.setImpressora(Impressora.builder().id(UUID.randomUUID()).build());h.setUnidades(1);h.setPecasPorImpressao(1);h.setTempoImpressaoMinutos(30L);h.setMarkupPercentual(BigDecimal.ZERO);h.setPrecoSugerido(new BigDecimal("30"));h.setPrecoVendaPlataforma(new BigDecimal("45"));h.setCustoTotal(new BigDecimal("30"));
  when(historicos.buscarPorIdComItens(h.getId())).thenReturn(Optional.of(h));
  var novo=new CalculoHistorico();novo.setNomePeca("Peça");novo.setNomeFilamento("PLA");novo.setItensFilamento(new ArrayList<>());novo.setCustoTotal(new BigDecimal("30"));novo.setPrecoSugerido(new BigDecimal("30"));
  when(calculos.calcular(any())).thenReturn(novo);when(calculos.calcularComPrecoFixo(any(),any())).thenReturn(novo);
  when(itens.saveAndFlush(any())).thenAnswer(i->{ItemPedido item=i.getArgument(0);item.setId(UUID.randomUUID());return item;});
  service.adicionar(p.getId(),h.getId(),1,0);
  assertThat(p.getItens().getFirst().getPrecoUnitario()).isEqualByComparingTo("45");
  assertThat(p.getRentabilidade().lucro()).isEqualByComparingTo("15");
  verify(calculos).calcularComPrecoFixo(any(),eq(new BigDecimal("45.00")));
 }
}
