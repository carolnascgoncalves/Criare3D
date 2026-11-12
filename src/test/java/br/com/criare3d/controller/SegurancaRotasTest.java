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
        @Bean LimiteLogin limiteLogin() { return new LimiteLogin(); }
        @Bean UserDetailsService users() { return nome -> User.withUsername(nome).password("unused").roles("USER").build(); }
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
        assertThat(acessar("/clientes/listar", Perfil.ADMIN)).isEqualTo(204);
    }
    @Test void saudeEPublicaMasOutrosEndpointsNao() throws Exception {
        assertThat(acessar("/actuator/health", null)).isEqualTo(204);
        assertThat(acessar("/actuator/env", Perfil.ADMIN)).isEqualTo(403);
    }
}
