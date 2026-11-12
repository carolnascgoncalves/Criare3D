package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NovasOperacoesTest {
    @Test void custoPersonalizadoNaoExigeFilamento() {
        var historicos=mock(CalculoHistoricoRepository.class);var custos=mock(CustoPersonalizadoRepository.class);
        var service=new CustoPersonalizadoService();ReflectionTestUtils.setField(service,"historicos",historicos);ReflectionTestUtils.setField(service,"custos",custos);
        var id=UUID.randomUUID();var lote=new CalculoHistorico();lote.setId(id);when(historicos.bloquearPorId(id)).thenReturn(Optional.of(lote));
        service.registrar(id," Acabamento ",new BigDecimal("12.50"));
        var captor=org.mockito.ArgumentCaptor.forClass(CustoPersonalizado.class);verify(custos).save(captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Acabamento");assertThat(captor.getValue().getValor()).isEqualByComparingTo("12.50");
    }
    @Test void custoNegativoNaoPodeSerSalvo() {
        var service=new CustoPersonalizadoService();
        assertThatIllegalArgumentException().isThrownBy(()->service.registrar(UUID.randomUUID(),"Tinta",new BigDecimal("-1")));
    }
    @Test void vendaParcialMantemPlanejamentoEFinalConclui() {
        var historicos=mock(CalculoHistoricoRepository.class);var vendas=mock(VendaCalculoRepository.class);var planos=mock(PlanejamentoProducaoRepository.class);
        var service=new VendaService(historicos,vendas,java.time.Clock.systemUTC(),planos,mock(ItemPedidoRepository.class));
        var c=new CalculoHistorico();var id=UUID.randomUUID();c.setId(id);c.setProduzido(true);c.setUnidades(3);c.setUnidadesVendidas(0);c.setCustoTotal(BigDecimal.ONE);c.setPrecoSugerido(BigDecimal.TEN);
        when(historicos.bloquearPorId(id)).thenReturn(Optional.of(c));when(historicos.findById(id)).thenReturn(Optional.of(c));
        assertThat(service.registrarVenda(id,2)).isEqualTo(ResultadoOperacao.OK);verifyNoInteractions(planos);
        assertThat(service.registrarVenda(id,1)).isEqualTo(ResultadoOperacao.OK);verify(planos).concluirPorCalculo(id);
    }
}
