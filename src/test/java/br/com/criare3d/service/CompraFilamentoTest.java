package br.com.criare3d.service;

import br.com.criare3d.dto.CompraFilamentoRequest;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.repository.FilamentoRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CompraFilamentoTest {
    @ParameterizedTest
    @CsvSource(value = {
            "1000, 1000, 1000, 95.50, 2000, 87.75",
            "250, 1000, 1000, 100, 1250, 96.00",
            "0, 1000, 1000, 95.50, 1000, 95.50",
            "500, 250, 500, 60, 750, 46.67",
            "0, padrao, 500, 60, 500, 60.00"
    }, nullValues = "padrao")
    void compraPreservaValorDoEstoqueEPonderaPorGramas(
            String saldoAnterior, String gramasCompradas, String pesoComprado,
            String precoComprado, String saldoEsperado, String custoMedioEsperado) {
        var repository = mock(FilamentoRepository.class);
        var service = new FilamentoServiceImp(repository, null, null);
        var filamento = new Filamento();
        var id = UUID.randomUUID();
        filamento.setId(id);
        filamento.setEstoqueGramas(new BigDecimal(saldoAnterior));
        filamento.setPesoRoloGramas(new BigDecimal("1000"));
        filamento.setPrecoRolo(new BigDecimal("80"));
        when(repository.findById(id)).thenReturn(Optional.of(filamento));
        var compra = new CompraFilamentoRequest();
        compra.setGramasCompradas(gramasCompradas == null ? null : new BigDecimal(gramasCompradas));
        compra.setPesoRoloGramas(new BigDecimal(pesoComprado));
        compra.setPrecoRolo(new BigDecimal(precoComprado));

        assertThat(service.registrarCompra(id, compra)).isTrue();
        assertThat(filamento.getEstoqueGramas()).isEqualByComparingTo(saldoEsperado);
        assertThat(filamento.getPesoRoloGramas()).isEqualByComparingTo(pesoComprado);
        assertThat(filamento.getPrecoRolo()).isEqualByComparingTo(custoMedioEsperado);
        verify(repository).saveAndFlush(filamento);
    }
}
