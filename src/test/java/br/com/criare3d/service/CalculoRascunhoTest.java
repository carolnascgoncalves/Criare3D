package br.com.criare3d.service;

import br.com.criare3d.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CalculoRascunhoTest {
    private final JsonMapper json = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    @Test
    void resultadoEParametrosVoltamDoJsonSemSessaoESemBytesDeFoto() {
        var alterados = new ArrayList<String>();
        var estado = new RascunhoCalculo(UUID.randomUUID(), new RascunhoCalculo.Dados(),
            (chave, valor) -> alterados.add(chave));
        var entrada = new CalculoRequest();
        entrada.setPecaId(UUID.randomUUID());
        entrada.setUnidades(7);
        entrada.setFotoPecaBytes(new byte[]{1, 2, 3});
        var resultado = new CalculoResponse();
        resultado.setDataCalculo(LocalDateTime.of(2026, 11, 17, 8, 30));
        resultado.setCustoTotal(new BigDecimal("7.12345678"));
        resultado.setItensFilamento(List.of(new ItemCalculoResponse()));
        estado.setAttribute("requestCalculo", entrada);
        estado.setAttribute("resultadoCalculo", resultado);
        UUID foto = UUID.randomUUID();
        estado.setAttribute("fotoCalculoId", foto);
        var carregado = json.readValue(json.writeValueAsString(estado.getDados()), RascunhoCalculo.Dados.class);
        assertThat(carregado.getRequestCalculo().getUnidades()).isEqualTo(7);
        assertThat(carregado.getRequestCalculo().getFotoPecaBytes()).isNull();
        assertThat(carregado.getResultadoCalculo().getCustoTotal()).isEqualByComparingTo("7.12345678");
        assertThat(carregado.getResultadoCalculo().getDataCalculo()).isEqualTo(resultado.getDataCalculo());
        assertThat(carregado.getFotoCalculoId()).isEqualTo(foto);
        assertThat(alterados).containsExactly("requestCalculo", "resultadoCalculo", "fotoCalculoId");
    }

    @Test
    void restauracaoDeFormularioNaoAceitaCustosInternosNemConteudoDeFoto() {
        UUID peca = UUID.randomUUID();
        UUID filamento = UUID.randomUUID();
        var formulario = new RascunhoFormulario(Map.of("pecaId", peca.toString(), "unidades", "5",
            "markupPercentual", "30", "custosPreservados.tarifa", "0", "precosAtuaisNoOrcamento", true,
            "referenciaCustosHistoricos", UUID.randomUUID().toString(), "fotoPecaBytes", "foto"),
            List.of(new RascunhoFormulario.Material(filamento.toString(), "18.5", true, true)), 2, 1L);
        var entrada = CalculoRascunhoService.formulario(formulario);
        assertThat(entrada.getPecaId()).isEqualTo(peca);
        assertThat(entrada.getUnidades()).isEqualTo(5);
        assertThat(entrada.getFilamentos().getFirst().getFilamentoId()).isEqualTo(filamento);
        assertThat(entrada.getFilamentos().getFirst().getQuantidadeFilamentoGramas()).isEqualByComparingTo("18.5");
        assertThat(entrada.getCustosPreservados()).isNull();
        assertThat(entrada.getReferenciaCustosHistoricos()).isNull();
        assertThat(entrada.isPrecosAtuaisNoOrcamento()).isFalse();
        assertThat(entrada.getFotoPecaBytes()).isNull();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void leituraEAlteracaoExigemContaEUsuarioEGravamSomenteACamadaAlterada() {
        var jdbc = mock(JdbcTemplate.class);
        var usuario = mock(UsuarioLogadoService.class);
        var conta = UUID.randomUUID();
        var usuarioId = UUID.randomUUID();
        var id = UUID.randomUUID();
        when(usuario.buscarTenantIdAtual()).thenReturn(conta);
        when(usuario.buscarIdUsuarioAtual()).thenReturn(usuarioId);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(new RascunhoCalculo.Dados()));
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        var service = new CalculoRascunhoService(jdbc, usuario, Clock.systemUTC());
        var estado = service.abrir(id, false);
        estado.setAttribute("formulario", new RascunhoFormulario(Map.of("unidades", "5"), List.of(), 1, 1L));
        var invocacoes = mockingDetails(jdbc).getInvocations();
        var leitura = invocacoes.stream().filter(i -> i.getMethod().getName().equals("query")).findFirst().orElseThrow();
        assertThat(leitura.getArgument(0, String.class)).contains("id=? AND tenant_id=? AND usuario_id=?");
        assertThat((Object[]) leitura.getRawArguments()[2]).contains(id, conta, usuarioId);
        var gravacao = invocacoes.stream().filter(i -> i.getMethod().getName().equals("update")).findFirst().orElseThrow();
        assertThat(gravacao.getArgument(0, String.class)).contains("jsonb_set", "tenant_id=? AND usuario_id=?");
        var argumentos = (Object[]) gravacao.getRawArguments()[1];
        assertThat(argumentos).contains(id, conta, usuarioId);
        assertThat(argumentos[0]).isEqualTo("formulario");
        assertThat(argumentos[1].toString()).contains("unidades").doesNotContain("resultadoCalculo", "fotoCalculoId");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        assertThatIllegalArgumentException().isThrownBy(() -> service.abrir(UUID.randomUUID(), false))
            .withMessageContaining("Rascunho não encontrado");
    }

    @Test
    void jsonIncluidoNoHtmlEscapaFechamentoDeScript() {
        var estado = new RascunhoCalculo();
        estado.setAttribute("formulario", new RascunhoFormulario(
            Map.of("nome", "</script><img src=x onerror=alert(1)>"), List.of(), 1, 1L));
        var service = new CalculoRascunhoService(mock(JdbcTemplate.class), mock(UsuarioLogadoService.class), Clock.systemUTC());
        assertThat(service.formularioJson(estado)).doesNotContain("<", ">", "</script>").contains("\\u003c");
    }
}
