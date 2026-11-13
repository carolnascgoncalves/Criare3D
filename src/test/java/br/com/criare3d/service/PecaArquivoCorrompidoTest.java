package br.com.criare3d.service;

import br.com.criare3d.models.Peca;
import br.com.criare3d.repository.PecaArquivoPlacaRepository;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PecaArquivoCorrompidoTest {
    @Test void jsonInvalidoPreservaCausaSemImpedirCalculoManual() {
        var service = new PecaArquivoService(mock(PecaArquivoPlacaRepository.class));
        assertThatThrownBy(() -> service.preparar("{invalido"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasCauseInstanceOf(tools.jackson.core.JacksonException.class);
        var peca = new Peca();
        peca.setDadosArquivo3mf("{invalido");
        assertThat(service.buscarMateriaisParaCalculo(peca)).isEmpty();
    }
}
