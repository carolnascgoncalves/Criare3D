package br.com.criare3d.service;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class AgendaPedidoTest {
    private final AgendaPedido agenda = AgendaPedido.de(LocalTime.of(8,0), LocalTime.of(18,0), "1,2,3,4,5");

    @Test void quantidadeUsaTempoPorUnidadeEIncluiAquecimentoEmCadaImpressao() {
        var inicio = LocalDateTime.of(2026,11,12,8,0);
        var p = agenda.prever(inicio,29,5,5,3,10,0);
        // 3*29+5, intervalo 10, 2*29+5 = 165 minutos.
        assertThat(p.fim()).isEqualTo(inicio.plusMinutes(165));
        assertThat(p.impressoes()).hasSize(2);
        assertThat(p.impressoes().getFirst().unidades()).isEqualTo(3);
        assertThat(p.impressoes().getLast().unidades()).isEqualTo(2);
        assertThat(p.impressoes().getLast().inicio()).isEqualTo(inicio.plusMinutes(102));
    }

    @Test void impressaoPodeTerminarANoiteMasTrocaEsperaProximoExpediente() {
        var p = agenda.prever(LocalDateTime.of(2026,11,13,17,0),120,0,2,1,10,0);
        assertThat(p.inicio()).isEqualTo(LocalDateTime.of(2026,11,13,17,0));
        assertThat(p.fim()).isEqualTo(LocalDateTime.of(2026,11,16,10,0));
    }

    @Test void folgaEArredondadaParaCimaSemAdicionarIntervaloAposUltimaImpressao() {
        var inicio=LocalDateTime.of(2026,11,12,8,0);
        assertThat(agenda.prever(inicio,1,0,1,1,10,10).fim()).isEqualTo(inicio.plusMinutes(2));
    }

    @Test void foraDoExpedienteComecaNoProximoDiaPermitido() {
        assertThat(agenda.proximoInicio(LocalDateTime.of(2026,11,14,12,0)))
                .isEqualTo(LocalDateTime.of(2026,11,16,8,0));
    }

    @Test void impedeJornadasInvalidasEQuantidadesQueNaoTerminariam() {
        assertThatIllegalArgumentException().isThrownBy(() -> AgendaPedido.de(LocalTime.NOON,LocalTime.of(8,0),"1"));
        assertThatIllegalArgumentException().isThrownBy(() -> agenda.prever(LocalDateTime.now(),0,0,1,1,0,0));
        assertThatIllegalArgumentException().isThrownBy(() -> agenda.prever(LocalDateTime.now(),1,0,1,0,0,0));
    }
}
