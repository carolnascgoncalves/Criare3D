package br.com.criare3d.config;

import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.security.UsuarioPrincipal;
import br.com.criare3d.service.TenantTemaCache;
import br.com.criare3d.dto.TenantTemaProjection;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Impede contas sem tenant de acessar telas que consultam ou gravam dados de cliente. */
public class TenantObrigatorioFilter extends OncePerRequestFilter {

    private final TenantTemaCache tenantTemaCache;

    public TenantObrigatorioFilter(TenantTemaCache tenantTemaCache) {
        this.tenantTemaCache = tenantTemaCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || !(autenticacao.getPrincipal() instanceof UsuarioPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (principal.getTenantId() != null) {
            TenantTemaProjection tema = tenantTemaCache.buscarTema(principal.getTenantId()).orElse(null);
            if (tema == null || !Boolean.TRUE.equals(tema.getAtivo())) {
                encerrarSessao(request);
                response.sendRedirect(request.getContextPath() + "/login?suspenso");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (rotaPermitidaSemTenant(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (principal.getPerfil() == Perfil.ADMIN && request.getMethod().equals("GET")) {
            response.sendRedirect(request.getContextPath() + "/clientes/listar");
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private void encerrarSessao(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var sessao = request.getSession(false);
        if (sessao != null) sessao.invalidate();
    }

    private boolean rotaPermitidaSemTenant(String caminho) {
        return caminho.equals("/login")
                || caminho.equals("/actuator/health")
                || caminho.startsWith("/actuator/")
                || caminho.equals("/recuperar-senha")
                || caminho.startsWith("/recuperar-senha/")
                || caminho.equals("/logout")
                || caminho.equals("/favicon.ico")
                || caminho.equals("/conta")
                || caminho.equals("/senha")
                || caminho.startsWith("/senha/")
                || caminho.startsWith("/usuarios/")
                || caminho.equals("/clientes")
                || caminho.startsWith("/clientes/")
                || caminho.startsWith("/vendor/")
                || caminho.startsWith("/css/")
                || caminho.startsWith("/js/")
                || caminho.startsWith("/images/");
    }
}
