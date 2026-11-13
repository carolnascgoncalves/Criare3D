package br.com.criare3d.controller;
import br.com.criare3d.service.Modelo3mfParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class Modelo3mfErroTest {
    @Test void devolveMensagemOriginalDoLimiteNaConsulta() throws Exception {
        var parser=mock(Modelo3mfParser.class);var arquivo=new MockMultipartFile("arquivo3mf","teste.3mf","application/octet-stream",new byte[]{1});
        when(parser.analisarConsulta(arquivo)).thenThrow(new IllegalArgumentException("Os metadados do arquivo são grandes demais para analisar."));
        var resposta=new Modelo3mfController(parser).analisarConsulta(arquivo);
        assertThat(resposta.getStatusCode().value()).isEqualTo(422);
        assertThat(resposta.getBody()).isEqualTo(Map.of("erro","Os metadados do arquivo são grandes demais para analisar."));
    }
}
