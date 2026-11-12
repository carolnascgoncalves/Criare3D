package br.com.criare3d.controller;

import br.com.criare3d.security.AvisoLogin;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class AvisoLoginTest {
    @Test void avisoUsaAPaginaDeLoginEExpiraSemReiniciarContagem() {
        Instant agora = Instant.parse("2026-11-12T01:00:00Z");
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(AvisoLogin.BLOQUEADO_ATE, agora.plusSeconds(120));
        var model = new ExtendedModelMap(); var response = new MockHttpServletResponse();
        assertThat(new LoginController(Clock.fixed(agora.plusSeconds(45), ZoneOffset.UTC)).login(request,response,model)).isEqualTo("login");
        assertThat(model.get("esperaLogin")).isEqualTo(75L);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        new LoginController(Clock.fixed(agora.plusSeconds(121), ZoneOffset.UTC)).login(request,response,model);
        assertThat(model.get("esperaLogin")).isEqualTo(0L);
        assertThat(request.getSession().getAttribute(AvisoLogin.BLOQUEADO_ATE)).isNull();
    }
}
