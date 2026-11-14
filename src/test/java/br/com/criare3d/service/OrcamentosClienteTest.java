package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrcamentosClienteTest {
    @Test void listaDoClientePreparaItensEProducaoAntesDeRetornarATela() {
        var pedidos=mock(PedidoOrcamentoRepository.class);
        var compradores=mock(CompradorRepository.class);
        var service=new OrcamentoPedidoService(pedidos,mock(ItemPedidoRepository.class),mock(CalculoHistoricoRepository.class),mock(CalculoService.class),mock(EntradaPedidoService.class),java.time.Clock.systemUTC(),mock(FotoPedidoService.class),mock(PlanejamentoPedidoService.class));
        ReflectionTestUtils.setField(service,"compradores",compradores);
        var cliente=UUID.randomUUID();when(compradores.existsById(cliente)).thenReturn(true);
        var pedido=mock(PedidoOrcamento.class);var item=new ItemPedido();var lote=mock(CalculoHistorico.class);item.setLoteProducao(lote);
        when(pedido.getItens()).thenReturn(List.of(item));
        var pagina=new PageImpl<>(List.of(pedido));
        when(pedidos.buscarDoComprador(eq(cliente),eq(false),eq("teste"),isNull(),any())).thenReturn(pagina);
        assertThat(service.listarDoComprador(cliente," teste ",0,false,null)).isSameAs(pagina);
        verify(pedido).getItens();verify(lote).getProduzido();
    }
}
