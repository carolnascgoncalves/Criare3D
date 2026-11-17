package br.com.criare3d.api.auth;

import br.com.criare3d.exception.RegraNegocioException;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

public class ApiBearerFilter extends OncePerRequestFilter {
    private final ApiJwtService jwt;
    private final ApiContas contas;
    private final ApiAuthService auth;

    public ApiBearerFilter(ApiJwtService jwt, ApiContas contas, ApiAuthService auth) {
        this.jwt = jwt;
        this.contas = contas;
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() > 8192)
                throw ApiAuthService.invalido();
            var token = jwt.ler(header.substring(7));
            if (token.getSubject() == null || token.getClaimAsString("sid") == null)
                throw ApiAuthService.invalido();
            var usuario =
                    contas.porId(UUID.fromString(token.getSubject()))
                            .orElseThrow(ApiAuthService::invalido);
            UUID familia = UUID.fromString(token.getClaimAsString("sid"));
            String tenant = token.getClaimAsString("tenant");
            if (!Objects.equals(
                            tenant,
                            usuario.getTenantId() == null ? null : usuario.getTenantId().toString())
                    || !ApiJwtService.credencial(usuario).equals(token.getClaimAsString("cred"))
                    || !auth.familiaAtiva(familia, usuario.getUsuarioId()))
                throw ApiAuthService.invalido();
            String caminho = request.getRequestURI().substring(request.getContextPath().length());
            if (usuario.isTrocaSenhaObrigatoria()
                    && !Set.of("/api/v1/auth/me", "/api/v1/auth/senha", "/api/v1/auth/logout")
                            .contains(caminho)) {
                ApiRespostaSeguranca.enviar(
                        response,
                        HttpStatus.FORBIDDEN,
                        "troca_senha_obrigatoria",
                        "Altere sua senha antes de continuar.");
                return;
            }
            if (usuario.getTenantId() == null && !caminho.startsWith("/api/v1/auth/")) {
                ApiRespostaSeguranca.enviar(
                        response,
                        HttpStatus.FORBIDDEN,
                        "conta_sem_tenant",
                        "Esta conta não possui uma loja vinculada.");
                return;
            }
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(
                                    usuario, null, usuario.getAuthorities()));
            request.setAttribute("api.familia", familia);
        } catch (RegraNegocioException e) {
            SecurityContextHolder.clearContext();
            ApiRespostaSeguranca.enviar(response, e.getStatus(), e.getCodigo(), e.getMessage());
            return;
        } catch (org.springframework.dao.DataAccessException e) {
            SecurityContextHolder.clearContext();
            ApiRespostaSeguranca.enviar(
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "autenticacao_indisponivel",
                    "Não foi possível validar a sessão agora.");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            ApiRespostaSeguranca.enviar(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "token_invalido",
                    "Token inválido ou expirado.");
            return;
        }
        chain.doFilter(request, response);
    }
}
