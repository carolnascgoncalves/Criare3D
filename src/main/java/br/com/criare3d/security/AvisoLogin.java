package br.com.criare3d.security;

import jakarta.servlet.http.*;
import java.time.Instant;

/** Guarda apenas a expiração do aviso; nunca guarda a senha nem a inclui na URL. */
public final class AvisoLogin {
    public static final String BLOQUEADO_ATE = "login.bloqueadoAte";
    private AvisoLogin() {}

    public static void bloquear(HttpServletRequest request, HttpServletResponse response, Instant ate, long segundos) {
        request.getSession(true).setAttribute(BLOQUEADO_ATE, ate);
        response.setHeader("Retry-After", Long.toString(segundos));
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", request.getContextPath() + "/login?bloqueado");
    }
}
