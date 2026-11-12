package br.com.criare3d.service;

import br.com.criare3d.dto.AjusteEstoqueCalculoResponse;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.VendaCalculoRepository;
import br.com.criare3d.service.interfaces.FilamentoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProducaoLoteVendidoTest {
    private final CalculoHistoricoRepository historicos = mock(CalculoHistoricoRepository.class);
    private final VendaCalculoRepository vendas = mock(VendaCalculoRepository.class);
    private final FilamentoService filamentos = mock(FilamentoService.class);
    private final br.com.criare3d.repository.ItemPedidoRepository reservas=mock(br.com.criare3d.repository.ItemPedidoRepository.class);
    private final ProducaoService service = new ProducaoService(historicos, vendas, filamentos, java.time.Clock.systemUTC(),reservas);
    private final UUID id = UUID.randomUUID();
    private final UUID filamentoId = UUID.randomUUID();

    private CalculoHistorico preparar(int unidadesVendidas) {
        CalculoHistorico calculo = new CalculoHistorico();
        calculo.setId(id);
        calculo.setProduzido(true);
        calculo.setUnidades(3);
        calculo.setUnidadesVendidas(unidadesVendidas);
        when(historicos.bloquearPorId(id)).thenReturn(Optional.of(calculo));
        // A flag legado pode estar falsa mesmo havendo venda parcial.
        when(historicos.buscarDadosParaAtualizarStatus(id)).thenReturn(List.of(
                new AjusteEstoqueCalculoResponse(id, true, false, filamentoId,
                        new BigDecimal("30"), null, null)));
        return calculo;
    }

    @Test
    void naoDesfazLoteDeTresComDuasUnidadesVendidas() {
        CalculoHistorico calculo = preparar(2);
        assertThat(service.atualizarProduzido(id, false)).isEqualTo(ResultadoOperacao.VENDA_ATIVA);
        assertThat(calculo.getUnidadesVendidas()).isEqualTo(2);
        verify(historicos, never()).atualizarStatus(any(), anyBoolean(), anyBoolean());
        verifyNoInteractions(filamentos, vendas);
    }
    @Test void naoDesfazProducaoDePecasReservadasMesmoSemVendas() {
        var c=preparar(0);when(reservas.reservadas(id)).thenReturn(2L);
        assertThat(service.atualizarProduzido(id,false)).isEqualTo(ResultadoOperacao.RESERVA_ATIVA);
        assertThat(c.getProduzido()).isTrue();verifyNoInteractions(filamentos,vendas);
    }

    @Test
    void registroDeVendaTambemProtegeLoteComContadorInconsistente() {
        preparar(0);
        when(vendas.existeVendaAtiva(id)).thenReturn(true);
        assertThat(service.atualizarProduzido(id, false)).isEqualTo(ResultadoOperacao.VENDA_ATIVA);
        verify(historicos, never()).atualizarStatus(any(), anyBoolean(), anyBoolean());
        verifyNoInteractions(filamentos);
    }

    @Test
    void permiteDesfazerLoteSemVendasEDevolveSomenteOFilamentoConsumido() {
        CalculoHistorico calculo = preparar(0);
        Filamento filamento = new Filamento();
        filamento.setId(filamentoId);
        filamento.setEstoqueGramas(new BigDecimal("100"));
        when(filamentos.fetchByIds(any())).thenReturn(List.of(filamento));
        assertThat(service.atualizarProduzido(id, false)).isEqualTo(ResultadoOperacao.OK);
        assertThat(filamento.getEstoqueGramas()).isEqualByComparingTo("130");
        assertThat(calculo.getProduzido()).isFalse();
        assertThat(calculo.getDataProducao()).isNull();
        verify(vendas, never()).deleteByCalculo_Id(any());
    }
    @Test
    void estoqueInsuficienteNaoProduzNemConsomeSaldo() {
        CalculoHistorico calculo = preparar(0);
        calculo.setProduzido(false);
        when(historicos.buscarDadosParaAtualizarStatus(id)).thenReturn(List.of(
                new AjusteEstoqueCalculoResponse(id, false, false, filamentoId,
                        new BigDecimal("30"), null, null)));
        Filamento filamento = new Filamento();
        filamento.setId(filamentoId);
        filamento.setEstoqueGramas(new BigDecimal("10"));
        when(filamentos.fetchByIds(any())).thenReturn(List.of(filamento));
        assertThat(service.atualizarProduzido(id, true)).isEqualTo(ResultadoOperacao.ESTOQUE_INSUFICIENTE);
        assertThat(calculo.getProduzido()).isFalse();
        assertThat(filamento.getEstoqueGramas()).isEqualByComparingTo("10");
    }
}
