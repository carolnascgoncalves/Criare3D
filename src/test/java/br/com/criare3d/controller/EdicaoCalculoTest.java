package br.com.criare3d.controller;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.service.CalculoService;
import br.com.criare3d.service.FotoRascunhoService;
import br.com.criare3d.service.HistoricoCalculoService;
import org.junit.jupiter.api.Test;
import br.com.criare3d.dto.RascunhoCalculo;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EdicaoCalculoTest {
    private final CalculoService calculos = mock(CalculoService.class);
    private final HistoricoCalculoService historicos = mock(HistoricoCalculoService.class);
    private final FotoRascunhoService fotos = mock(FotoRascunhoService.class);
    private final CalculoHistoricoController controller = new CalculoHistoricoController(
        null, calculos, null, historicos, null, null, null, null, null, null, null);

    @Test
    void editarPreservaParametrosEFotoPorIdSemGuardarBytesNaSessao() {
        var dados = new CalculoRequest();
        dados.setUnidades(5);
        dados.setTempoImpressaoMinutos(29L);
        dados.setFotoPecaBytes(new byte[]{1, 2, 3});
        var session = new RascunhoCalculo();
        var fotoId = UUID.randomUUID();
        session.setAttribute("fotoCalculoId", fotoId);
        var resposta = controller.editar(dados, session);
        assertThat(resposta.getStatusCode().value()).isEqualTo(303);
        assertThat(resposta.getHeaders().getLocation().toString()).isEqualTo("/historico/novo?editar=true&rascunhoId=" + session.getId());
        var rascunho = (CalculoRequest) session.getAttribute("calculoParaEditar");
        assertThat(rascunho.getUnidades()).isEqualTo(5);
        assertThat(rascunho.getTempoImpressaoMinutos()).isEqualTo(29);
        assertThat(rascunho.getFotoPecaBytes()).isNull();
        assertThat(session.getAttribute("fotoCalculoId")).isEqualTo(fotoId);
        verifyNoInteractions(calculos, historicos);
    }

    @Test
    void custoAlteradoPedeRevisaoAntesDeSalvarHistorico() {
        ReflectionTestUtils.setField(controller, "fotoRascunhoService", fotos);
        when(fotos.buscar(null)).thenReturn(Optional.empty());
        var dados = new CalculoRequest();
        var session = new RascunhoCalculo();
        var anterior = new CalculoResponse();
        anterior.setCustoTotal(BigDecimal.ONE);
        anterior.setPrecoSugerido(BigDecimal.TEN);
        session.setAttribute("resultadoCalculo", anterior);
        var novo = new CalculoHistorico();
        novo.setCustoTotal(new BigDecimal("2"));
        novo.setPrecoSugerido(BigDecimal.TEN);
        novo.setItensFilamento(new ArrayList<>());
        when(calculos.calcular(dados)).thenReturn(novo);
        var resposta = controller.salvar(dados, new BeanPropertyBindingResult(dados, "calculo"), session);
        assertThat(resposta.getViewName()).isEqualTo("redirect:/historico/resultado?valoresAtualizados=true");
        assertThat(((CalculoResponse) session.getAttribute("resultadoCalculo")).getCustoTotal()).isEqualByComparingTo("2");
        verifyNoInteractions(historicos);
    }
}
