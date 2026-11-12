package br.com.criare3d.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class LimiteLoginFilter extends OncePerRequestFilter {
    private final LimiteLogin limite;
    public LimiteLoginFilter(LimiteLogin limite) { this.limite = limite; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath())) {
            // Cabeçalhos enviados pelo cliente não são uma fonte confiável de IP.
            long espera = limite.esperaSegundos(request.getParameter("email"), request.getRemoteAddr());
            if (espera > 0) {
                AvisoLogin.bloquear(request, response,
                        limite.bloqueadoAte(request.getParameter("email"), request.getRemoteAddr()), espera);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
