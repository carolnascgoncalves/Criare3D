package br.com.criare3d.controller;

import br.com.criare3d.service.TenantTemaCache;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NavegacaoRecursosTest {
    @Test
    void formularioComErroContinuaRecebendoRecursosDeCalculoNasRotasPost() {
        var advice = new NavegacaoModelAdvice(mock(TenantTemaCache.class));
        for (var rota : new String[]{"/historico/novo", "/historico/calcular", "/historico/salvar"}) {
            var request = new MockHttpServletRequest();
            request.setServletPath(rota);
            var model = new ExtendedModelMap();
            advice.adicionarPaginaAtiva(request, model);
            assertThat(model.get("paginaAtiva")).as(rota).isEqualTo("calculo");
        }
        var request = new MockHttpServletRequest();
        request.setServletPath("/historico/resultado");
        var model = new ExtendedModelMap();
        advice.adicionarPaginaAtiva(request, model);
        assertThat(model.get("paginaAtiva")).isEqualTo("resultado-calculo");
    }
}
