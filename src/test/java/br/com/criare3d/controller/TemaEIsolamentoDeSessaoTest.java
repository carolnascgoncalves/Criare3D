package br.com.criare3d.controller;

import br.com.criare3d.config.TenantObrigatorioFilter;
import br.com.criare3d.dto.TenantTemaProjection;
import br.com.criare3d.models.Tenant;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.security.UsuarioPrincipal;
import br.com.criare3d.service.TenantTemaCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemaEIsolamentoDeSessaoTest {

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void naoEntregaTemaDeOutroCliente() {
        UUID tenantUsuario = UUID.randomUUID();
        UsuarioPrincipal principal = principalDoTenant(tenantUsuario);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        TenantTemaCache cache = mock(TenantTemaCache.class);
        TemaController controller = new TemaController(cache);

        assertThat(controller.css(UUID.randomUUID(), 1).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void encerraSessaoAtivaQuandoClienteFoiDesativado() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UsuarioPrincipal principal = principalDoTenant(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        TenantTemaProjection tema = mock(TenantTemaProjection.class);
        when(tema.getAtivo()).thenReturn(false);
        TenantTemaCache cache = mock(TenantTemaCache.class);
        when(cache.buscarTema(tenantId)).thenReturn(Optional.of(tema));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/");
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean cadeiaExecutada = new AtomicBoolean();

        new TenantObrigatorioFilter(cache).doFilter(request, response,
                (req, res) -> cadeiaExecutada.set(true));

        assertThat(cadeiaExecutada).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?suspenso");
        assertThat(session.isInvalid()).isTrue();
    }

    private UsuarioPrincipal principalDoTenant(UUID tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("cliente@teste.local")
                .password("hash")
                .perfil(Perfil.USER)
                .tenant(tenant)
                .build();
        return new UsuarioPrincipal(usuario);
    }
}
