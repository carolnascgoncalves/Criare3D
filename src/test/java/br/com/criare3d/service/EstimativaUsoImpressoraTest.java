package br.com.criare3d.service;

import br.com.criare3d.repository.CalculoHistoricoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EstimativaUsoImpressoraTest {
    private final CalculoHistoricoRepository historico = mock(CalculoHistoricoRepository.class);
    private final EstimativaUsoImpressoraService service = new EstimativaUsoImpressoraService(historico,
        Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void semHistoricoUsaReferenciaEditavelSemConsultarMarketplace() {
        var a1 = service.estimar("Bambu Lab A1", null);
        assertThat(a1.horasAteSubstituicao()).isEqualByComparingTo("3000");
        assertThat(a1.horasMensais()).isEqualByComparingTo("100");
        assertThat(a1.horasMensaisPadrao()).isTrue();
        assertThat(service.estimar("Bambu Lab P1S", null).horasAteSubstituicao()).isEqualByComparingTo("5000");
    }

    @Test
    void novaImpressoraReaproveitaUsoRecenteDaLoja() {
        when(historico.somarMinutosMaquinaProduzidosDesde(any())).thenReturn(9015L);
        var uso = service.estimar("Bambu Lab A1 mini", null);
        assertThat(uso.horasMensais()).isEqualByComparingTo(new BigDecimal("150.25"));
        assertThat(uso.horasMensaisPadrao()).isFalse();
        verify(historico).somarMinutosMaquinaProduzidosDesde(java.time.LocalDateTime.parse("2026-10-18T12:00:00"));
    }

    @Test
    void impressoraExistenteUsaSeuProprioHistorico() {
        UUID id = UUID.randomUUID();
        when(historico.somarMinutosMaquinaProduzidosDaImpressoraDesde(any(), eq(id))).thenReturn(120L);
        assertThat(service.estimar("P1S", id).horasMensais()).isEqualByComparingTo("2");
        verify(historico, never()).somarMinutosMaquinaProduzidosDesde(any());
    }
}
