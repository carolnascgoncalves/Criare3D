package br.com.criare3d.config;

import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.security.UsuarioPrincipal;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || !(autenticacao.getPrincipal() instanceof UsuarioPrincipal principal)
                || principal.getTenantId() != null || rotaPermitidaSemTenant(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (principal.getPerfil() == Perfil.ADMIN && request.getMethod().equals("GET")) {
            response.sendRedirect(request.getContextPath() + "/clientes/listar");
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean rotaPermitidaSemTenant(String caminho) {
        return caminho.equals("/login")
                || caminho.equals("/logout")
                || caminho.equals("/favicon.ico")
                || caminho.equals("/conta")
                || caminho.equals("/senha")
                || caminho.startsWith("/senha/")
                || caminho.startsWith("/usuarios/")
                || caminho.equals("/clientes")
                || caminho.startsWith("/clientes/")
                || caminho.startsWith("/css/")
                || caminho.startsWith("/js/")
                || caminho.startsWith("/images/");
    }
}
