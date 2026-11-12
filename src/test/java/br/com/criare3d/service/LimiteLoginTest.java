package br.com.criare3d.service;

import br.com.criare3d.security.LimiteLogin;
import br.com.criare3d.security.LimiteLoginFilter;
import br.com.criare3d.security.SenhaBcrypt;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class LimiteLoginTest {
    @Test void limitaContaMesmoQuandoTrocaDeIpEOrigemMesmoQuandoTrocaDeConta() {
        LimiteLogin limite = new LimiteLogin(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 5; i++) limite.falhou(" Pessoa@exemplo.com ", "1");
        assertThat(limite.esperaSegundos("pessoa@exemplo.com", "2")).isEqualTo(30);
        assertThat(limite.esperaSegundos("outra@exemplo.com", "1")).isEqualTo(30);
        limite.autenticou("pessoa@exemplo.com");
        assertThat(limite.esperaSegundos("pessoa@exemplo.com", "2")).isZero();
        assertThat(limite.esperaSegundos("outra@exemplo.com", "1")).isEqualTo(30);
    }

    @Test void bloqueioExpiraEPenalidadeCresce() {
        Instant inicio = Instant.now().minusSeconds(31);
        Clock clock = org.mockito.Mockito.mock(Clock.class);
        org.mockito.Mockito.when(clock.instant()).thenReturn(inicio);
        LimiteLogin limite = new LimiteLogin(clock);
        for (int i = 0; i < 5; i++) limite.falhou("a", "1");
        org.mockito.Mockito.when(clock.instant()).thenReturn(inicio.plusSeconds(31));
        assertThat(limite.esperaSegundos("a", "1")).isZero();
        limite.falhou("a", "1");
        assertThat(limite.esperaSegundos("a", "1")).isEqualTo(60);
    }

    @Test void filtroInterrompeAntesDeAutenticar() throws Exception {
        LimiteLogin limite = new LimiteLogin();
        for (int i = 0; i < 5; i++) limite.falhou("a", "127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login"); request.addParameter("email", "a");
        request.addHeader("X-Forwarded-For", "outro-ip-enviado-pelo-cliente");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new LimiteLoginFilter(limite).doFilter(request, response, (a,b) -> {
            throw new AssertionError("Não deveria tentar autenticar durante o bloqueio");
        });
        assertThat(response.getStatus()).isEqualTo(303);
        assertThat(response.getHeader("Location")).isEqualTo("/login?bloqueado");
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(request.getSession().getAttribute(br.com.criare3d.security.AvisoLogin.BLOQUEADO_ATE)).isInstanceOf(Instant.class);
        assertThat(response.getHeader("Retry-After")).isNotBlank();
    }

    @Test void consideraBytesDeCaracteresAcentuados() {
        var validador = new SenhaBcrypt.Validador();
        assertThat(validador.isValid("a".repeat(72), null)).isTrue();
        assertThat(validador.isValid("á".repeat(36), null)).isTrue();
        assertThat(validador.isValid("á".repeat(37), null)).isFalse();
    }
}
