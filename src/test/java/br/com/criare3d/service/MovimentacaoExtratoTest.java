package br.com.criare3d.service;

import br.com.criare3d.dto.FiltroMovimentacao;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Movimentacao;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.repository.MovimentacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovimentacaoExtratoTest {
    @Test
    void exportacaoBuscaNomesEmLoteEInformaQuandoPassaDoLimite() {
        var repository = mock(MovimentacaoRepository.class);
        var filamentos = mock(FilamentoRepository.class);
        var service = new MovimentacaoService();
        ReflectionTestUtils.setField(service, "movimentos", repository);
        ReflectionTestUtils.setField(service, "filamentos", filamentos);
        var id = UUID.randomUUID();
        var m = new Movimentacao();
        m.setFilamentoId(id);
        var f = new Filamento();
        f.setId(id);
        f.setNome("PLA");
        f.setCor("Amarelo");
        when(repository.extrato(any(), any(), anyString(), anyString(), anyBoolean(), anyString(),
            isNull(), eq(true), any())).thenReturn(new PageImpl<>(List.of(m, m), PageRequest.of(0, 5000), 5001));
        when(filamentos.findAllById(List.of(id))).thenReturn(List.of(f));

        var exportacao = service.exportar(new FiltroMovimentacao(LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30), "", "", "", null, false, true));

        assertThat(exportacao.limitada()).isTrue();
        assertThat(exportacao.nomesFilamentos()).containsEntry(id, "PLA · Amarelo");
        verify(filamentos, times(1)).findAllById(List.of(id));
        verify(filamentos, never()).findById(any());
    }

    @Test
    void resumoSomaVendasEEstornosSemSomarUnidadesComGramas() {
        var gramas = mock(MovimentacaoRepository.ResumoMedida.class);
        var unidades = mock(MovimentacaoRepository.ResumoMedida.class);
        when(gramas.getPecasVendidas()).thenReturn(BigDecimal.ZERO);
        when(unidades.getPecasVendidas()).thenReturn(new BigDecimal("3"));
        var extrato = new MovimentacaoService.Extrato(new PageImpl<>(List.of()),
            List.of(gramas, unidades), null, null);
        assertThat(extrato.pecasVendidas()).isEqualByComparingTo("3");
    }

    @Test
    void datasInvertidasSaoRecusadasExcetoQuandoTodoOPeriodoFoiEscolhido() {
        var inicio = LocalDate.of(2026, 11, 30);
        var fim = LocalDate.of(2026, 11, 1);
        assertThatIllegalArgumentException().isThrownBy(() ->
            new FiltroMovimentacao(inicio, fim, "", "", "", null, false, false));
        var filtro = new FiltroMovimentacao(inicio, fim, "invalido", "invalida", " PLA ", null, false, true);
        assertThat(filtro.tudo()).isTrue();
        assertThat(filtro.tipo()).isEmpty();
        assertThat(filtro.direcao()).isEmpty();
        assertThat(filtro.busca()).isEqualTo("PLA");
    }
}
