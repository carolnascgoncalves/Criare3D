package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.StatusPedido;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OperacaoPedidoTest {
    private final PedidoOrcamentoRepository pedidos=mock(PedidoOrcamentoRepository.class);
    private final ItemPedidoRepository itens=mock(ItemPedidoRepository.class);
    private final CalculoHistoricoRepository historicos=mock(CalculoHistoricoRepository.class);
    private final ImpressoraRepository impressoras=mock(ImpressoraRepository.class);
    private final CalculoService calculos=mock(CalculoService.class);
    private final HistoricoCalculoService salvar=mock(HistoricoCalculoService.class);
    private final PlanejamentoPedidoService planejamento=mock(PlanejamentoPedidoService.class);
    private final ProducaoService producao=mock(ProducaoService.class);
    private final VendaService vendas=mock(VendaService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"),ZoneId.of("America/Sao_Paulo"));
    private final OperacaoPedidoService service=new OperacaoPedidoService(pedidos,itens,historicos,impressoras,calculos,salvar,new EntradaPedidoService(),planejamento,producao,vendas,clock,mock(FotoPedidoService.class));

    private PedidoOrcamento pedido(StatusPedido status) {
        var p=new PedidoOrcamento();p.setId(UUID.randomUUID());p.setCliente("Cliente");p.setStatus(status);
        var h=new CalculoHistorico();h.setId(UUID.randomUUID());h.setUnidades(3);h.setUnidadesVendidas(0);h.setProduzido(true);
        h.setCustoTotal(new BigDecimal("4.00"));
        var i=new ItemPedido();i.setId(UUID.randomUUID());i.setPedido(p);i.setNome("Peça");i.setQuantidade(2);i.setUsarEstoque(2);
        i.setLoteEstoque(h);i.setPrecoUnitario(new BigDecimal("10.00"));i.setCusto(new BigDecimal("8.00"));p.getItens().add(i);
        when(pedidos.bloquear(p.getId())).thenReturn(Optional.of(p));when(historicos.bloquearPorId(h.getId())).thenReturn(Optional.of(h));
        return p;
    }
    @Test void aprovarProntasApenasReservaSemProduzirVenderOuDescontarFilamento() {
        var p=pedido(StatusPedido.ENVIADO);
        service.aprovar(p.getId(),0);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.PRONTO);
        assertThat(p.getItens().getFirst().getReservaEstoque()).isEqualTo(2);
        verifyNoInteractions(producao,vendas,calculos,salvar,planejamento);
    }
    @Test void reservaConcorrenteImpedeAprovacaoAcimaDoSaldo() {
        var p=pedido(StatusPedido.ENVIADO);when(itens.reservadas(p.getItens().getFirst().getLoteEstoque().getId())).thenReturn(2L);
        assertThatIllegalArgumentException().isThrownBy(()->service.aprovar(p.getId(),0)).withMessageContaining("estoque");
        assertThat(p.getStatus()).isEqualTo(StatusPedido.ENVIADO);verifyNoInteractions(vendas,producao);
    }
    @Test void repeticaoEFormularioAntigoNaoReaprovamPedido() {
        var p=pedido(StatusPedido.ENVIADO);service.aprovar(p.getId(),0);
        assertThatIllegalArgumentException().isThrownBy(()->service.aprovar(p.getId(),0)).withMessageContaining("outra aba");
    }
    @Test void entregaLiberaReservaERegistraVendaSomenteDoPedidoComFreteEDesconto() {
        var p=pedido(StatusPedido.PRONTO);var i=p.getItens().getFirst();i.setReservaEstoque(2);
        p.setFrete(new BigDecimal("6.00"));p.setCustoFrete(new BigDecimal("4.00"));p.setDesconto(new BigDecimal("2.00"));
        when(vendas.registrarVendaDoPedido(any(),anyInt(),any(),any(),any(),any())).thenReturn(ResultadoOperacao.OK);
        service.entregar(p.getId(),0);
        verify(vendas).registrarVendaDoPedido(eq(i.getLoteEstoque().getId()),eq(2),argThat(v->v.compareTo(new BigDecimal("13"))==0),argThat(v->v.compareTo(BigDecimal.ONE)==0),argThat(v->v.compareTo(new BigDecimal("2"))==0),eq(p.getId()));
        assertThat(i.getReservaEstoque()).isZero();assertThat(p.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
        assertThat(p.getPagoEm()).isNull();
    }
    @Test void horarioPassadoNaoConcluiProducaoOuPermiteEntrega() {
        var p=pedido(StatusPedido.APROVADO);var i=p.getItens().getFirst();i.setUsarEstoque(0);p.setPrevisao(LocalDateTime.of(2020,1,1,8,0));
        assertThatIllegalArgumentException().isThrownBy(()->service.entregar(p.getId(),0)).withMessageContaining("produção");
        verifyNoInteractions(vendas,producao);
    }
    @Test void cancelarPreservaProducaoEPagamentoEReverteSomenteVendasDoPedido() {
        var p=pedido(StatusPedido.ENTREGUE);p.setPagoEm(LocalDateTime.now(clock));
        var i=p.getItens().getFirst();i.setReservaEstoque(2);
        service.cancelar(p.getId(),0);
        verify(vendas).cancelarPedido(p.getId());verifyNoInteractions(producao);
        assertThat(i.getLoteEstoque().getProduzido()).isTrue();assertThat(i.getReservaEstoque()).isZero();
        assertThat(p.getStatus()).isEqualTo(StatusPedido.CANCELADO);assertThat(p.getPagoEm()).isNotNull();
    }
    @Test void pagamentoNaoRegistraVendaNemEntrega() {
        var p=pedido(StatusPedido.APROVADO);service.pagar(p.getId(),0);
        assertThat(p.getStatus()).isEqualTo(StatusPedido.APROVADO);assertThat(p.getPagoEm()).isNotNull();
        verifyNoInteractions(vendas,producao);
    }
    @Test void dinheiroDoBancoComOitoCasasMasSomenteCentavosContinuaEditavel() {
        assertThatCode(()->OrcamentoPedidoService.validarDinheiro(new BigDecimal("12.34000000"))).doesNotThrowAnyException();
        assertThatIllegalArgumentException().isThrownBy(()->OrcamentoPedidoService.validarDinheiro(new BigDecimal("12.341")));
    }
}
