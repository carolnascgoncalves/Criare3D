package br.com.criare3d.service;

import br.com.criare3d.dto.DashboardResumo;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.service.interfaces.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardDadosTest {
    @Test
    void reaproveitaConsultasSemCompartilharContasOuPeriodos() {
        var historico = mock(CalculoHistoricoRepository.class);
        var resumo = mock(DashboardService.class);
        var usuario = mock(UsuarioLogadoService.class);
        var pecas = mock(PecaService.class);
        var service = new DashboardDadosService(historico, resumo, usuario, pecas,
                mock(FilamentoService.class), mock(ImpressoraService.class), mock(EmbalagemService.class));
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        when(resumo.resumir(any(), any(), any())).thenReturn(
                new DashboardResumo(0, 0, 0, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO));
        when(usuario.buscarTenantIdAtual()).thenReturn(a);
        when(pecas.contar()).thenReturn(1L);
        var primeiro = service.buscar("30");
        assertThat(service.buscar("30")).isSameAs(primeiro);
        verify(pecas, times(1)).contar();
        when(usuario.buscarTenantIdAtual()).thenReturn(b);
        when(pecas.contar()).thenReturn(2L);
        assertThat(service.buscar("30").get("totalPecas")).isEqualTo(2L);
        when(usuario.buscarTenantIdAtual()).thenReturn(a);
        assertThat(service.buscar("30").get("totalPecas")).isEqualTo(1L);
        assertThat(service.buscar("90")).isNotSameAs(primeiro);
        verify(pecas, times(3)).contar();
        assertThatThrownBy(() -> primeiro.put("totalPecas", 99L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void naoConsultaNemCacheiaContaAusente() {
        var historico = mock(CalculoHistoricoRepository.class);
        var service = new DashboardDadosService(historico, mock(DashboardService.class),
                mock(UsuarioLogadoService.class), mock(PecaService.class), mock(FilamentoService.class),
                mock(ImpressoraService.class), mock(EmbalagemService.class));
        assertThatThrownBy(() -> service.buscar("30")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(historico);
    }

    @Test void outrasVendasEntramNoLucroSerieECategoriaSemAlterarHorasDeImpressao() {
        var historico=mock(CalculoHistoricoRepository.class);var base=mock(DashboardService.class);var usuario=mock(UsuarioLogadoService.class);
        var outras=mock(br.com.criare3d.repository.VendaProdutoLojaRepository.class);var resumo=mock(br.com.criare3d.repository.VendaProdutoLojaRepository.Resumo.class);
        var tenant=UUID.randomUUID();when(usuario.buscarTenantIdAtual()).thenReturn(tenant);
        when(base.resumir(any(),any(),any())).thenReturn(new DashboardResumo(1,1,1,BigDecimal.ONE,60,60,BigDecimal.TEN));
        when(resumo.getUnidades()).thenReturn(2L);when(resumo.getReceita()).thenReturn(new BigDecimal("5"));when(resumo.getLucro()).thenReturn(new BigDecimal("3"));when(resumo.getSemCusto()).thenReturn(0L);
        when(outras.resumir(any(),any())).thenReturn(resumo);
        var hoje=java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        when(historico.somarLucroVendidoPorDia(any(),any())).thenReturn(java.util.Collections.singletonList(new Object[]{hoje.getYear(),hoje.getMonthValue(),hoje.getDayOfMonth(),BigDecimal.ONE}));
        when(outras.lucroPorDia(any(),any())).thenReturn(java.util.Collections.singletonList(new Object[]{hoje.getYear(),hoje.getMonthValue(),hoje.getDayOfMonth(),new BigDecimal("3")}));
        var service=new DashboardDadosService(historico,base,usuario,mock(PecaService.class),mock(FilamentoService.class),mock(ImpressoraService.class),mock(EmbalagemService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"outrasVendas",outras);
        var fluxo=mock(CalculoHistoricoRepository.FluxoProducao.class);
        when(fluxo.getCalculadas()).thenReturn(51L);when(fluxo.getProduzidas()).thenReturn(23L);when(fluxo.getVendidas()).thenReturn(20L);
        when(historico.resumirFluxoProducao(any(),any())).thenReturn(fluxo);
        var dados=service.buscar("30");
        assertThat(dados.get("fluxoCalculados")).isEqualTo(51L);assertThat(dados.get("fluxoProduzidos")).isEqualTo(23L);
        assertThat(dados.get("fluxoVendidos")).isEqualTo(20L);assertThat(dados.get("fluxoPercentualVendido")).isEqualTo(87);
        assertThat((BigDecimal)dados.get("lucroVendido")).isEqualByComparingTo("4");assertThat((BigDecimal)dados.get("lucroPorHora")).isEqualByComparingTo("1");
        assertThat(dados.get("totalVendidos")).isEqualTo(1L);assertThat(dados.get("totalVendasLoja")).isEqualTo(3L);
        assertThat(dados.get("vendasCategoriaNomes").toString()).contains("Outros produtos (não 3D)");
        var valores=(java.util.List<?>)dados.get("valoresLucro");assertThat((BigDecimal)valores.get(valores.size()-1)).isEqualByComparingTo("4");
        service.invalidar(tenant);assertThat(service.buscar("30")).isNotSameAs(dados);
    }
}
