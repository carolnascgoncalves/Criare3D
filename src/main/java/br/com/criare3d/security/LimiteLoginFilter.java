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
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(espera));
                response.setHeader("Cache-Control", "no-store");
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("<!doctype html><html lang=\"pt-BR\"><meta charset=\"utf-8\">"
                        + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                        + "<title>Aguarde para entrar</title><main><h1>Aguarde um pouco</h1>"
                        + "<p>Muitas tentativas de acesso. Tente novamente em " + espera + " segundos.</p>"
                        + "<p><a href=\"login\">Voltar ao acesso</a></p></main></html>");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
