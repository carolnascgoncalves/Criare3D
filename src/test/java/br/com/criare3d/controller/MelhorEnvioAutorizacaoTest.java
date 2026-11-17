package br.com.criare3d.controller;

import br.com.criare3d.service.MelhorEnvioService;
import br.com.criare3d.service.OrcamentoPedidoService;
import br.com.criare3d.service.UsuarioLogadoService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MelhorEnvioAutorizacaoTest {
    @Test
    void callbackValidaStateUsuarioAmbienteEUsaAutorizacaoUmaUnicaVez() {
        var envio = mock(MelhorEnvioService.class);
        var usuario = mock(UsuarioLogadoService.class);
        var tenant = UUID.randomUUID();
        when(usuario.buscarTenantIdAtual()).thenReturn(tenant);
        when(usuario.buscarIdUsuarioAtual()).thenReturn(UUID.randomUUID());
        when(envio.ambiente()).thenReturn("sandbox");
        when(envio.autorizar(any())).thenReturn("https://sandbox.melhorenvio.com.br/oauth/authorize");
        var c = new MelhorEnvioController(envio, mock(OrcamentoPedidoService.class), usuario, Clock.systemUTC());
        var session = new MockHttpSession();
        c.conectar(session, new RedirectAttributesModelMap());
        var state = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(envio).autorizar(state.capture());
        assertThat(state.getValue()).hasSizeGreaterThanOrEqualTo(40);
        c.callback("state-errado", "codigo", null, session, new MockHttpServletResponse(), new RedirectAttributesModelMap());
        verify(envio, never()).conectar(any());
        c.conectar(session, new RedirectAttributesModelMap());
        verify(envio, times(2)).autorizar(state.capture());
        String correto = state.getValue();
        c.callback(correto, "codigo", null, session, new MockHttpServletResponse(), new RedirectAttributesModelMap());
        verify(envio).conectar("codigo");
        c.callback(correto, "codigo", null, session, new MockHttpServletResponse(), new RedirectAttributesModelMap());
        verify(envio, times(1)).conectar("codigo");
    }
}
