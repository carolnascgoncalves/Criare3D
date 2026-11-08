package br.com.criare3d.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PrimeiroAcessoSenhaObrigatoriaFilter extends OncePerRequestFilter {
    public static final String SESSAO_TROCA_OBRIGATORIA = "criare3d.trocaSenhaObrigatoria";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var session = request.getSession(false);
        boolean trocaObrigatoria = session != null
                && Boolean.TRUE.equals(session.getAttribute(SESSAO_TROCA_OBRIGATORIA));
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (trocaObrigatoria && autenticacao != null && autenticacao.isAuthenticated()
                && !permitidoDuranteTroca(request)) {
            response.sendRedirect(request.getContextPath() + "/senha/primeiro-acesso");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean permitidoDuranteTroca(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return caminho.equals("/senha/primeiro-acesso")
                || caminho.equals("/logout")
                || caminho.equals("/favicon.ico")
                || caminho.startsWith("/css/")
                || caminho.startsWith("/js/")
                || caminho.startsWith("/images/");
    }
}
