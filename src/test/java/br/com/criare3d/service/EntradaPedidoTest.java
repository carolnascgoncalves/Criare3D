package br.com.criare3d.service;
import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.PlataformaVenda;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class EntradaPedidoTest {
    private final EntradaPedidoService service=new EntradaPedidoService();
    @Test void loteComTresPorImpressaoPodeOriginarPedidoDeUmaUnidade() {
        var h=new CalculoHistorico();h.setPeca(Peca.builder().id(UUID.randomUUID()).build());
        h.setImpressora(Impressora.builder().id(UUID.randomUUID()).build());
        h.setUnidades(5);h.setPecasPorImpressao(3);h.setTempoImpressaoMinutos(29L);
        var entrada=service.ler(service.escrever(service.copiar(h)),1);
        assertThat(entrada.getUnidades()).isEqualTo(1);
        assertThat(entrada.getPecasPorImpressao()).isEqualTo(1);
        assertThat(entrada.getTempoImpressaoMinutos()).isEqualTo(29L);
        assertThat(h.getUnidades()).isEqualTo(5);
        assertThat(h.getPecasPorImpressao()).isEqualTo(3);
    }
    @Test void entradaPersistidaNaoCarregaFotoNemAlteraRascunhoDaCalculadora() {
        var r=new CalculoRequest();r.setPecaId(UUID.randomUUID());r.setUnidades(5);r.setPecasPorImpressao(3);
        r.setFotoPecaBytes(new byte[]{1,2,3});r.setFotoPecaContentType("image/png");
        var salvo=service.ler(service.escrever(r),2);
        assertThat(salvo.getFotoPecaBytes()).isNull();assertThat(salvo.getPecasPorImpressao()).isEqualTo(2);
        assertThat(r.getFotoPecaBytes()).hasSize(3);assertThat(r.getUnidades()).isEqualTo(5);
        assertThat(salvo.getPecaId()).isEqualTo(r.getPecaId());
    }
    @Test void plataformaLegadaTikTokNaoPerdeSuasTaxasAoVirarOrcamento() {
        var h=new CalculoHistorico();h.setPeca(Peca.builder().id(UUID.randomUUID()).build());
        h.setImpressora(Impressora.builder().id(UUID.randomUUID()).build());h.setPlataformaVenda("TIKTOK_SHOP");
        assertThat(service.copiar(h).getPlataformaVenda()).isEqualTo(PlataformaVenda.TIKTOK);
    }
    @Test void embalagemFixaOriginalSegueNoOrcamentoMesmoSemCadastroVinculado() {
        var h=new CalculoHistorico();h.setPeca(Peca.builder().id(UUID.randomUUID()).build());
        h.setImpressora(Impressora.builder().id(UUID.randomUUID()).build());h.setCustoEmbalagem(new java.math.BigDecimal("2.50"));
        var entrada=service.ler(service.escrever(service.copiar(h)),10);
        assertThat(entrada.getEmbalagemId()).isNull();
        assertThat(entrada.getCustoEmbalagemPersonalizada()).isEqualByComparingTo("2.50");
    }

    @Test void abrirResumoAntigoNaoPrecisaCarregarColecoesDoHistorico() {
        var lote=org.mockito.Mockito.mock(CalculoHistorico.class);
        org.mockito.Mockito.when(lote.getItensFilamento()).thenThrow(new org.hibernate.LazyInitializationException("Histórico fora da transação"));
        var item=new ItemPedido();item.setEntradaJson("{}");item.setQuantidade(1);item.setLoteEstoque(lote);
        assertThat(service.origemCustos(item)).isEqualTo("Valores do cálculo original");
        org.mockito.Mockito.verifyNoInteractions(lote);
    }

    @Test void recuperarCustosAntigosRecarregaHistoricoEmVezDePercorrerEntidadeDesconectada() {
        var id=UUID.randomUUID();var desconectado=org.mockito.Mockito.mock(CalculoHistorico.class);
        org.mockito.Mockito.when(desconectado.getId()).thenReturn(id);
        org.mockito.Mockito.when(desconectado.getItensFilamento()).thenThrow(new org.hibernate.LazyInitializationException("Desconectado"));
        var repositorio=org.mockito.Mockito.mock(br.com.criare3d.repository.CalculoHistoricoRepository.class);
        var original=new CalculoHistorico();original.setId(id);original.setCustoEmbalagem(new java.math.BigDecimal("2.50"));
        org.mockito.Mockito.when(repositorio.buscarPorIdComItens(id)).thenReturn(java.util.Optional.of(original));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"historicos",repositorio);
        var item=new ItemPedido();item.setEntradaJson("{}");item.setQuantidade(1);item.setLoteEstoque(desconectado);
        assertThat(service.ler(item,1).getCustosPreservados().embalagem()).isEqualByComparingTo("2.50");
        org.mockito.Mockito.verify(desconectado,org.mockito.Mockito.never()).getItensFilamento();
    }
}
