package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class VendasOperacaoTest {
 private final CalculoHistoricoRepository historicos=mock(CalculoHistoricoRepository.class);
 private final VendaCalculoRepository vendas=mock(VendaCalculoRepository.class);
 private final ItemPedidoRepository reservas=mock(ItemPedidoRepository.class);
 private final VendaService service=new VendaService(historicos,vendas,java.time.Clock.fixed(java.time.Instant.parse("2026-11-12T01:30:00Z"),java.time.ZoneId.of("America/Sao_Paulo")),mock(PlanejamentoProducaoRepository.class),reservas);
 private final UUID id=UUID.randomUUID();
 private CalculoHistorico lote(boolean produzido) {
  var c=new CalculoHistorico();c.setId(id);c.setUnidades(3);c.setUnidadesVendidas(0);c.setProduzido(produzido);
  c.setPrecoSugerido(new BigDecimal("20"));c.setCustoTotal(new BigDecimal("5"));
  when(historicos.bloquearPorId(id)).thenReturn(Optional.of(c));return c;
 }
 @Test void vendaNaoProduzAutomaticamente() {
  var c=lote(false);assertThat(service.registrarVenda(id,1)).isEqualTo(ResultadoOperacao.PRODUCAO_NECESSARIA);
  assertThat(c.getProduzido()).isFalse();verifyNoInteractions(vendas);
 }
 @Test void reservaDoPedidoImpedeVendaAvulsaAcimaDoSaldoLivre() {
  var c=lote(true);when(reservas.reservadas(id)).thenReturn(2L);
  assertThat(service.registrarVenda(id,2)).isEqualTo(ResultadoOperacao.RESERVA_ATIVA);
  assertThat(c.getUnidadesVendidas()).isZero();verifyNoInteractions(vendas);
  assertThat(service.registrarVenda(id,1)).isEqualTo(ResultadoOperacao.OK);
 }
 @Test void vendaDoPedidoNaoPodeSerCanceladaPelaOperacaoAvulsa() {
  var c=lote(true);c.setUnidadesVendidas(2);
  var v=new VendaCalculo();v.setId(UUID.randomUUID());v.setCalculo(c);v.setUnidades(2);v.setPedidoId(UUID.randomUUID());
  when(vendas.findById(v.getId())).thenReturn(Optional.of(v));
  when(vendas.findByCalculo_IdOrderByDataVendaDesc(id)).thenReturn(List.of(v));
  assertThat(service.cancelarVenda(id,v.getId(),1)).isEqualTo(ResultadoOperacao.PEDIDO_VINCULADO);
  assertThat(service.cancelarVendas(id)).isEqualTo(ResultadoOperacao.PEDIDO_VINCULADO);
  assertThat(v.getUnidadesAtivas()).isEqualTo(2);assertThat(c.getUnidadesVendidas()).isEqualTo(2);
 }
 @Test void registraPrecoDescontoETaxasPorUnidade() {
  var c=lote(true);assertThat(service.registrarVenda(id,2,new BigDecimal("18"),new BigDecimal("2"),new BigDecimal("3"))).isEqualTo(ResultadoOperacao.OK);
  var captor=org.mockito.ArgumentCaptor.forClass(VendaCalculo.class);verify(vendas).save(captor.capture());
  assertThat(captor.getValue().getLucroUnitario()).isEqualByComparingTo("8");
  assertThat(captor.getValue().getDataVenda()).isEqualTo(LocalDateTime.of(2026,11,11,22,30));
  assertThat(captor.getValue().isValoresReais()).isTrue();assertThat(c.getUnidadesVendidas()).isEqualTo(2);
 }
 @Test void saldoAtualDoLoteImpedeVenderAcimaDoDisponivel() {
  var c=lote(true);c.setUnidadesVendidas(2);assertThat(service.registrarVenda(id,2)).isEqualTo(ResultadoOperacao.QUANTIDADE_VENDA_ACIMA_DISPONIVEL);verifyNoInteractions(vendas);
 }
 @Test void cancelaUmaUnidadeSemApagarVendaNemDesproduzirLote() {
  var c=lote(true);c.setUnidadesVendidas(2);var v=new VendaCalculo();v.setId(UUID.randomUUID());v.setCalculo(c);v.setUnidades(2);v.setDataVenda(LocalDateTime.now());
  when(vendas.findById(v.getId())).thenReturn(Optional.of(v));when(vendas.findByCalculo_IdOrderByDataVendaDesc(id)).thenReturn(List.of(v));
  assertThat(service.cancelarVenda(id,v.getId(),1)).isEqualTo(ResultadoOperacao.OK);assertThat(v.getUnidadesAtivas()).isEqualTo(1);
  assertThat(c.getUnidadesVendidas()).isEqualTo(1);assertThat(c.getProduzido()).isTrue();verify(vendas,never()).deleteByCalculo_Id(any());
 }
 @Test void naoCancelaVendaDeOutroLote() {
  lote(true);var v=new VendaCalculo();var outro=new CalculoHistorico();outro.setId(UUID.randomUUID());v.setCalculo(outro);v.setUnidades(1);
  when(vendas.findById(any())).thenReturn(Optional.of(v));assertThat(service.cancelarVenda(id,UUID.randomUUID(),1)).isEqualTo(ResultadoOperacao.NAO_ENCONTRADO);
  assertThat(v.getUnidadesCanceladas()).isZero();
 }
 @Test void descontoMaiorQuePrecoNaoAlteraSaldo() {
  var c=lote(true);assertThat(service.registrarVenda(id,1,new BigDecimal("10"),new BigDecimal("11"),null)).isEqualTo(ResultadoOperacao.VALOR_VENDA_INVALIDO);
  assertThat(c.getUnidadesVendidas()).isZero();verify(vendas,never()).save(any());
 }
 @Test void filaIncluiAquecimentoIntervalosEFolga() {
  assertThat(PlanejamentoProducaoService.duracao(60,5,3,2,10,10)).isEqualTo(220);
  assertThat(PlanejamentoProducaoService.duracao(1,0,1,1,0,10)).isEqualTo(1);
 }
}
