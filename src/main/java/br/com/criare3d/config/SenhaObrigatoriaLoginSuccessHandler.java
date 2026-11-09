package br.com.criare3d.config;

import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.security.UsuarioPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SenhaObrigatoriaLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UsuarioRepository usuarioRepository;

    public SenhaObrigatoriaLoginSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        boolean trocaObrigatoria;
        if (authentication.getPrincipal() instanceof UsuarioPrincipal principal) {
            trocaObrigatoria = principal.isTrocaSenhaObrigatoria();
        } else {
            trocaObrigatoria = usuarioRepository.findByEmail(authentication.getName())
                    .map(usuario -> usuario.isTrocaSenhaObrigatoria())
                    .orElse(false);
        }
        if (trocaObrigatoria) {
            request.getSession(true).setAttribute(
                    PrimeiroAcessoSenhaObrigatoriaFilter.SESSAO_TROCA_OBRIGATORIA, true);
            response.sendRedirect(request.getContextPath() + "/senha/primeiro-acesso");
            return;
        }
        if (authentication.getPrincipal() instanceof UsuarioPrincipal principal
                && principal.getPerfil() == br.com.criare3d.models.enums.Perfil.ADMIN
                && principal.getTenantId() == null) {
            response.sendRedirect(request.getContextPath() + "/usuarios/listar");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/");
    }
}
