package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.StatusPedido;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OrcamentoCadastroTest {
    private final PedidoOrcamentoRepository pedidos=mock(PedidoOrcamentoRepository.class);
    private final ItemPedidoRepository itens=mock(ItemPedidoRepository.class);
    private final CalculoHistoricoRepository historicos=mock(CalculoHistoricoRepository.class);
    private final CalculoService calculos=mock(CalculoService.class);
    private final FotoPedidoService fotos=mock(FotoPedidoService.class);
    private final OrcamentoPedidoService service=new OrcamentoPedidoService(pedidos,itens,historicos,calculos,new EntradaPedidoService(),Clock.systemUTC(),fotos,mock(PlanejamentoPedidoService.class));

    private CalculoHistorico referencia(boolean produzido) {
        var h=new CalculoHistorico();h.setId(UUID.randomUUID());h.setProduzido(produzido);h.setUnidades(3);h.setUnidadesVendidas(0);
        h.setPeca(Peca.builder().id(UUID.randomUUID()).build());h.setImpressora(Impressora.builder().id(UUID.randomUUID()).build());
        h.setNomePeca("Peça com foto");h.setNomeFilamento("PLA");h.setTempoImpressaoMinutos(29L);h.setPecasPorImpressao(3);
        h.setCustoTotal(new BigDecimal("4.00"));h.setPrecoSugerido(new BigDecimal("10.00"));h.setTempoAquecimentoMinutos(0);h.setMinutosMaquinaLote(87L);
        when(historicos.buscarPorIdComItens(h.getId())).thenReturn(Optional.of(h));
        doAnswer(a->{PedidoOrcamento p=a.getArgument(0);p.setId(UUID.randomUUID());return p;}).when(pedidos).save(any());
        doAnswer(a->{ItemPedido i=a.getArgument(0);i.setId(UUID.randomUUID());return i;}).when(itens).saveAndFlush(any());
        when(calculos.calcular(any())).thenReturn(h);when(calculos.calcularComPrecoFixo(any(),any())).thenReturn(h);
        return h;
    }
    @Test void itemRecebeIdAntesDeCopiarFotoDeLoteComVariasUnidades() {
        var h=referencia(true);var p=service.iniciar(h.getId(),2,1);var i=p.getItens().getFirst();
        assertThat(i.getId()).isNotNull();assertThat(i.getQuantidade()).isEqualTo(2);assertThat(i.getProduzir()).isEqualTo(1);
        var ordem=inOrder(itens,fotos);ordem.verify(itens).saveAndFlush(i);ordem.verify(fotos).copiar(i.getId(),h.getId());
    }
    @Test void adicionaVariasPecasComQuantidadesIndependentes(){
        var a=referencia(true);var b=referencia(false);var dados=new br.com.criare3d.dto.SelecaoPedidoRequest();
        var x=new br.com.criare3d.dto.SelecaoPedidoRequest.Item();x.setSelecionado(true);x.setReferencia(a.getId());x.setQuantidade(2);x.setEstoque(1);
        var y=new br.com.criare3d.dto.SelecaoPedidoRequest.Item();y.setSelecionado(true);y.setReferencia(b.getId());y.setQuantidade(3);dados.setItens(List.of(x,y));
        var p=service.adicionarVarios(null,dados);assertThat(p.getItens()).hasSize(2);assertThat(p.getItens().get(0).getQuantidade()).isEqualTo(2);assertThat(p.getItens().get(1).getQuantidade()).isEqualTo(3);
    }
    @Test void semProducaoNaoPermiteUsarUmaPecaDoEstoque() {
        var h=referencia(false);
        assertThatIllegalArgumentException().isThrownBy(()->service.iniciar(h.getId(),1,1)).withMessageContaining("quantidade disponível");
        verifyNoInteractions(calculos,fotos);verify(itens,never()).saveAndFlush(any());
    }
    @Test void estoqueConsideraVendasEReservasDeOutrosPedidos() {
        var h=referencia(true);h.setUnidadesVendidas(1);when(itens.reservadas(h.getId())).thenReturn(1L);
        assertThatIllegalArgumentException().isThrownBy(()->service.iniciar(h.getId(),2,2));
        assertThat(service.disponivel(h)).isEqualTo(1);
    }
    @Test void arquivarRascunhoERestaurarNaoAlteraSeuEstadoComercial() {
        var p=new PedidoOrcamento();p.setId(UUID.randomUUID());when(pedidos.bloquear(p.getId())).thenReturn(Optional.of(p));
        service.arquivar(p.getId(),true);assertThat(p.isArquivado()).isTrue();assertThat(p.getStatus()).isEqualTo(StatusPedido.RASCUNHO);
        service.arquivar(p.getId(),false);assertThat(p.isArquivado()).isFalse();
    }
    @Test void naoEscondePedidoOperacionalComReservas() {
        var p=new PedidoOrcamento();p.setId(UUID.randomUUID());p.setStatus(StatusPedido.APROVADO);when(pedidos.bloquear(p.getId())).thenReturn(Optional.of(p));
        assertThatIllegalArgumentException().isThrownBy(()->service.arquivar(p.getId(),true));assertThat(p.isArquivado()).isFalse();
    }
    @Test void filtroDeStatusVaiParaConsultaPaginada(){
        when(pedidos.buscar(eq("cliente"),eq(false),eq(StatusPedido.ENTREGUE),any())).thenReturn(org.springframework.data.domain.Page.empty());
        assertThat(service.listar("cliente",0,false,StatusPedido.ENTREGUE)).isEmpty();
        verify(pedidos).buscar(eq("cliente"),eq(false),eq(StatusPedido.ENTREGUE),any());
    }
}
