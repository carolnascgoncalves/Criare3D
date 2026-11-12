package br.com.criare3d.service;

import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.PlataformaVenda;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MotorCalculoTest {
    @Test void calculaLoteSemBancoMantendoTempoPorUnidadeESnapshot() {
        var request = new CalculoRequest();
        var peca = new Peca(); peca.setId(UUID.randomUUID()); peca.setNome("Peça");
        var impressora = new Impressora(); impressora.setId(UUID.randomUUID()); impressora.setNome("Impressora");
        impressora.setPotenciaWatts(new BigDecimal("100")); impressora.setTempoAquecimentoMinutos(5);
        var filamento = new Filamento(); filamento.setId(UUID.randomUUID()); filamento.setNome("PLA");
        filamento.setPesoRoloGramas(new BigDecimal("1000")); filamento.setPrecoRolo(new BigDecimal("100"));
        var item = new ItemCalculoRequest(); item.setFilamentoId(filamento.getId()); item.setQuantidadeFilamentoGramas(BigDecimal.TEN);
        request.setPecaId(peca.getId()); request.setImpressoraId(impressora.getId()); request.setFilamentos(List.of(item));
        request.setTempoImpressaoMinutos(29L); request.setUnidades(5); request.setPecasPorImpressao(3);
        request.setMarkupPercentual(new BigDecimal("30")); request.setPlataformaVenda(PlataformaVenda.NENHUMA);
        var config = ConfiguracaoPadrao.criar(); config.setEmbalagemFixa(true);
        config.setCustoEmbalagemFixa(new BigDecimal("2")); config.setTarifaEnergiaKwh(BigDecimal.ONE);
        var agora = LocalDateTime.of(2026, 11, 12, 12, 0);
        var motor = new MotorCalculo(new CalculoPlataformaService());
        var resultado = motor.calcular(request, peca, impressora, config, Map.of(filamento.getId(), filamento), null, BigDecimal.ZERO, agora);
        assertThat(resultado.getTempoImpressaoMinutos()).isEqualTo(29);
        assertThat(resultado.getMinutosMaquinaLote()).isEqualTo(155);
        assertThat(resultado.getFornadas()).isEqualTo(2);
        assertThat(resultado.getCustoTotal()).isEqualByComparingTo("3.05166667");
        assertThat(resultado.getGramasTotaisLote()).isEqualByComparingTo("50");
        assertThat(resultado.getDataCalculo()).isEqualTo(agora);
        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getItensFilamento().getFirst().getCalculoHistorico()).isSameAs(resultado);
        assertThat(request.getTempoImpressaoMinutos()).isEqualTo(29);
    }

    @Test void limitaTamanhoDoRascunhoAntesDeCarregarDados() {
        var request = new CalculoRequest(); request.setPecaId(UUID.randomUUID()); request.setImpressoraId(UUID.randomUUID());
        request.setFilamentos(Collections.nCopies(101, new ItemCalculoRequest()));
        assertThatIllegalArgumentException().isThrownBy(() -> CalculoValidador.validarRequisicao(request))
                .withMessageContaining("100 filamentos");
    }
}
