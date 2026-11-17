package br.com.criare3d.controller;

import br.com.criare3d.config.CalculoRascunhoWeb;
import br.com.criare3d.dto.RascunhoCalculo;
import br.com.criare3d.service.CalculoRascunhoService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RascunhoWebTest {
    public void pagina(RascunhoCalculo estado) {
    }

    @Test
    void identificadorExplicitoERepassadoAoServiceEAosRedirecionamentos() throws Exception {
        var service = mock(CalculoRascunhoService.class);
        var web = new CalculoRascunhoWeb(service);
        var id = UUID.randomUUID();
        var estado = new RascunhoCalculo(id, new RascunhoCalculo.Dados(), (k, v) -> {});
        when(service.abrir(id, false)).thenReturn(estado);
        var request = new MockHttpServletRequest("GET", "/historico/resultado");
        request.setServletPath("/historico/resultado");
        request.setParameter("rascunhoId", id.toString());
        var parametro = new MethodParameter(getClass().getMethod("pagina", RascunhoCalculo.class), 0);
        assertThat(web.supportsParameter(parametro)).isTrue();
        assertThat(web.resolveArgument(parametro, new ModelAndViewContainer(), new ServletWebRequest(request), null))
            .isSameAs(estado);
        var redirect = new ModelAndView("redirect:/historico/resultado?valoresAtualizados=true");
        web.postHandle(request, new MockHttpServletResponse(), new Object(), redirect);
        assertThat(redirect.getViewName()).contains("valoresAtualizados=true", "rascunhoId=" + id);
        verify(service, times(1)).abrir(id, false);
    }
}
