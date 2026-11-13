package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OutrasVendasTest {
    private final ProdutoLojaRepository produtos=mock(ProdutoLojaRepository.class);
    private final VendaProdutoLojaRepository vendas=mock(VendaProdutoLojaRepository.class);
    private final EmbalagemRepository embalagens=mock(EmbalagemRepository.class);
    private final DashboardDadosService dashboard=mock(DashboardDadosService.class);
    private final UUID tenant=UUID.randomUUID(),id=UUID.randomUUID();
    private OutrasVendasService service() {
        var s=new OutrasVendasService();var usuario=mock(UsuarioLogadoService.class);when(usuario.buscarTenantIdAtual()).thenReturn(tenant);
        ReflectionTestUtils.setField(s,"embalagens",embalagens);ReflectionTestUtils.setField(s,"produtos",produtos);ReflectionTestUtils.setField(s,"vendas",vendas);ReflectionTestUtils.setField(s,"dashboard",dashboard);ReflectionTestUtils.setField(s,"usuario",usuario);
        ReflectionTestUtils.setField(s,"clock",Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"),ZoneId.of("America/Sao_Paulo")));
        return s;
    }
    private ProdutoLoja produto() {
        var p=new ProdutoLoja();p.setId(id);p.setNome("Luz de LED");p.setPreco(new BigDecimal("2.50"));p.setCusto(new BigDecimal("1"));when(produtos.findById(id)).thenReturn(Optional.of(p));return p;
    }
    @Test void vendaPreservaPrecoCustoENomeEAtualizaDashboardAoCancelar() {
        var s=service();var p=produto();var v=s.vender(id,2,null,null,LocalDate.of(2026,11,13));
        p.setNome("Outro produto");p.setPreco(new BigDecimal("10"));
        assertThat(v.getNomeProduto()).isEqualTo("Luz de LED");assertThat(v.getReceita()).isEqualByComparingTo("5");assertThat(v.getLucro()).isEqualByComparingTo("3");
        var vendaId=UUID.randomUUID();when(vendas.bloquear(vendaId)).thenReturn(Optional.of(v));s.cancelar(vendaId,1);
        assertThat(v.getAtivas()).isEqualTo(1);assertThat(v.getReceita()).isEqualByComparingTo("2.50");assertThat(v.getLucro()).isEqualByComparingTo("1.50");
        verify(dashboard,times(2)).invalidar(tenant);
        assertThatIllegalArgumentException().isThrownBy(()->s.cancelar(vendaId,2));assertThat(v.getCanceladas()).isEqualTo(1);
    }
    @Test void custoDesconhecidoNaoViraLucroFicticioEZeroExplicitoPermiteLucro() {
        var s=service();produto().setCusto(null);
        var sem=s.vender(id,1,null,null,LocalDate.of(2026,11,13));assertThat(sem.getReceita()).isEqualByComparingTo("2.50");assertThat(sem.getLucro()).isNull();
        assertThat(s.vender(id,1,null,BigDecimal.ZERO,LocalDate.of(2026,11,13)).getLucro()).isEqualByComparingTo("2.50");
    }
    @Test void produtoArquivadoOuDeOutraContaNaoPermiteVenda() {
        var s=service();produto().setArquivado(true);assertThatIllegalArgumentException().isThrownBy(()->s.vender(id,1,null,null,null));
        when(produtos.findById(id)).thenReturn(Optional.empty());assertThatIllegalArgumentException().isThrownBy(()->s.vender(id,1,null,null,null));verifyNoInteractions(vendas);
    }

    @Test void embalagemPersonalizadaDescontaPorUnidadeECancelamentoAcompanhaQuantidade() {
        var s=service();produto();
        var v=s.vender(id,2,null,null,null,VendaProdutoLoja.TipoEmbalagem.PERSONALIZADA,null,new BigDecimal("0.50"));
        assertThat(v.getReceita()).isEqualByComparingTo("5");assertThat(v.getLucro()).isEqualByComparingTo("2");
        when(vendas.bloquear(id)).thenReturn(Optional.of(v));s.cancelar(id,1);assertThat(v.getLucro()).isEqualByComparingTo("1");
        verifyNoInteractions(embalagens);
    }
    @Test void embalagemCadastradaPreservaNomeEValorOriginal() {
        var s=service();produto();var e=new Embalagem();e.setId(UUID.randomUUID());e.setNome("Saquinho");e.setCustoUnitario(new BigDecimal("0.50"));
        when(embalagens.findById(e.getId())).thenReturn(Optional.of(e));
        var v=s.vender(id,1,null,null,null,VendaProdutoLoja.TipoEmbalagem.CADASTRADA,e.getId(),null);
        e.setNome("Outro nome");e.setCustoUnitario(BigDecimal.TEN);
        assertThat(v.getNomeEmbalagem()).isEqualTo("Saquinho");assertThat(v.getCustoEmbalagem()).isEqualByComparingTo("0.50");assertThat(v.getLucro()).isEqualByComparingTo("1");
    }
    @Test void rejeitaEmbalagemInvalidaArquivadaOuDeOutraConta() {
        var s=service();produto();
        assertThatIllegalArgumentException().isThrownBy(()->s.vender(id,1,null,null,null,VendaProdutoLoja.TipoEmbalagem.PERSONALIZADA,null,null));
        assertThatIllegalArgumentException().isThrownBy(()->s.vender(id,1,null,null,null,VendaProdutoLoja.TipoEmbalagem.PERSONALIZADA,null,new BigDecimal("-1")));
        when(embalagens.findById(id)).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(()->s.vender(id,1,null,null,null,VendaProdutoLoja.TipoEmbalagem.CADASTRADA,id,null));
        var e=new Embalagem();e.setArquivado(true);when(embalagens.findById(id)).thenReturn(Optional.of(e));
        assertThatIllegalArgumentException().isThrownBy(()->s.vender(id,1,null,null,null,VendaProdutoLoja.TipoEmbalagem.CADASTRADA,id,null));
        verifyNoInteractions(vendas);
    }
}
