package br.com.criare3d.controller;

import br.com.criare3d.config.*;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.security.*;
import br.com.criare3d.service.TenantTemaCache;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SegurancaRotasTest {
    @Configuration @EnableWebSecurity @Import(SecurityConfig.class)
    static class Contexto {
        @Bean TenantTemaCache tenantTemaCache() { return mock(TenantTemaCache.class); }
        @Bean SenhaObrigatoriaLoginSuccessHandler successHandler() { return mock(SenhaObrigatoriaLoginSuccessHandler.class); }
        @Bean java.time.Clock clock() {
            var clock = mock(java.time.Clock.class);
            org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-11-12T01:00:00Z"));
            return clock;
        }
        @Bean LimiteLogin limiteLogin(java.time.Clock clock) { return new LimiteLogin(clock); }
        @Bean UserDetailsService users() {
            String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4).encode("SenhaCorreta123!");
            return nome -> User.withUsername(nome).password(hash).roles("USER").build();
        }
    }

    private int acessar(String caminho, Perfil perfil) throws Exception {
        try (var contexto = new AnnotationConfigWebApplicationContext()) {
            contexto.setServletContext(new MockServletContext()); contexto.register(Contexto.class); contexto.refresh();
            var request = new MockHttpServletRequest("GET", caminho); request.setServletPath(caminho);
            if (perfil != null) {
                var usuario = Usuario.builder().id(UUID.randomUUID()).email("teste@example.test").password("hash").perfil(perfil).build();
                var principal = new UsuarioPrincipal(usuario);
                var autenticacao = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
                request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(autenticacao));
            }
            var response = new MockHttpServletResponse();
            contexto.getBean(FilterChainProxy.class).doFilter(request, response, (a,b) -> ((jakarta.servlet.http.HttpServletResponse)b).setStatus(204));
            return response.getStatus();
        }
    }

    @Test void arquivosDoLoginSaoPublicos() throws Exception {
        assertThat(acessar("/vendor/bootstrap-icons/bootstrap-icons.min.css", null)).isEqualTo(204);
        assertThat(acessar("/vendor/turbo.es2017-umd.js", null)).isEqualTo(204);
        assertThat(acessar("/login", null)).isEqualTo(204);
    }
    @Test void administracaoExigeAdmin() throws Exception {
        assertThat(acessar("/usuarios/listar", Perfil.USER)).isEqualTo(403);
        assertThat(acessar("/clientes/listar", Perfil.USER)).isEqualTo(403);
        assertThat(acessar("/clientes/diagnostico-origem", Perfil.USER)).isEqualTo(403);
        assertThat(acessar("/clientes/diagnostico-origem", Perfil.ADMIN)).isEqualTo(204);
        assertThat(acessar("/clientes/listar", Perfil.ADMIN)).isEqualTo(204);
    }
    @Test void saudeEPublicaMasOutrosEndpointsNao() throws Exception {
        assertThat(acessar("/actuator/health", null)).isEqualTo(204);
        assertThat(acessar("/actuator/env", Perfil.ADMIN)).isEqualTo(403);
    }

    @Test void senhaCorretaAutenticaDepoisDaEsperaSemSerRejeitadaPeloLimitador() throws Exception {
        try (var contexto = new AnnotationConfigWebApplicationContext()) {
            contexto.setServletContext(new MockServletContext()); contexto.register(Contexto.class); contexto.refresh();
            var chain = contexto.getBean(FilterChainProxy.class);
            var get = new MockHttpServletRequest("GET", "/login"); get.setServletPath("/login");
            var token = new java.util.concurrent.atomic.AtomicReference<org.springframework.security.web.csrf.CsrfToken>();
            chain.doFilter(get, new MockHttpServletResponse(), (a,b) -> {
                var csrf = (org.springframework.security.web.csrf.CsrfToken)a.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
                csrf.getToken(); token.set(csrf);
            });
            var session = (MockHttpSession)get.getSession();
            for (int i = 0; i < 5; i++) {
                var falha = enviarLogin(chain,session,token.get(),"SenhaErrada123!");
                assertThat(falha.getHeader("Location")).isEqualTo(i < 4 ? "/login?erro" : "/login?bloqueado");
            }
            var bloqueado = enviarLogin(chain,session,token.get(),"SenhaCorreta123!");
            assertThat(bloqueado.getStatus()).isEqualTo(303);
            assertThat(bloqueado.getHeader("Location")).isEqualTo("/login?bloqueado");
            org.mockito.Mockito.verifyNoInteractions(contexto.getBean(SenhaObrigatoriaLoginSuccessHandler.class));
            org.mockito.Mockito.when(contexto.getBean(java.time.Clock.class).instant()).thenReturn(java.time.Instant.parse("2026-11-12T01:00:31Z"));
            var sucesso = enviarLogin(chain,session,token.get(),"SenhaCorreta123!");
            assertThat(sucesso.getStatus()).isEqualTo(200);
            org.mockito.Mockito.verify(contexto.getBean(SenhaObrigatoriaLoginSuccessHandler.class))
                    .onAuthenticationSuccess(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.argThat(auth -> auth.isAuthenticated()));
        }
    }

    private MockHttpServletResponse enviarLogin(FilterChainProxy chain, MockHttpSession session,
            org.springframework.security.web.csrf.CsrfToken csrf, String senha) throws Exception {
        var request = new MockHttpServletRequest("POST", "/login"); request.setServletPath("/login"); request.setSession(session);
        request.addParameter("email", "admin@example.test"); request.addParameter("password", senha);
        request.addParameter(csrf.getParameterName(), csrf.getToken());
        var response = new MockHttpServletResponse();
        chain.doFilter(request, response, (a,b) -> { throw new AssertionError("POST de login não deve alcançar o controller"); });
        return response;
    }
}
