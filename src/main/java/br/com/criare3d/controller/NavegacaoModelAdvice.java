package br.com.criare3d.controller;

import br.com.criare3d.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavegacaoModelAdvice {

    @ModelAttribute
    public void adicionarPaginaAtiva(HttpServletRequest request, Model model) {
        String caminho = request.getServletPath();
        String pagina = "";
        if (caminho.equals("/") || caminho.equals("/home")) pagina = "inicio";
        else if (caminho.startsWith("/dashboard")) pagina = "dashboard";
        else if (caminho.startsWith("/peca")) pagina = "pecas";
        else if (caminho.startsWith("/filamento")) pagina = "filamentos";
        else if (caminho.startsWith("/impressora")) pagina = "impressoras";
        else if (caminho.startsWith("/embalagem")) pagina = "embalagens";
        else if (caminho.startsWith("/historico")) pagina = "historico";
        else if (caminho.startsWith("/usuarios") || caminho.startsWith("/clientes")) pagina = "clientes";
        model.addAttribute("paginaAtiva", pagina);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        UsuarioPrincipal principal = autenticacao != null && autenticacao.isAuthenticated()
                && autenticacao.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal
                ? usuarioPrincipal : null;

        String nomeTenant = principal == null || principal.getTenantNome() == null
                || principal.getTenantNome().isBlank() ? "Criare 3D" : principal.getTenantNome();
        long temaVersao = principal == null ? 1L : Math.max(1L, principal.getTemaVersao());
        model.addAttribute("tenantDisponivel", principal != null && principal.getTenantId() != null);
        model.addAttribute("tenantNome", nomeTenant);
        model.addAttribute("tenantTemLogo", principal != null && principal.isTenantTemLogo());
        model.addAttribute("tenantLogoUrl", principal != null && principal.getTenantId() != null
                && principal.isTenantTemLogo()
                ? request.getContextPath() + "/tema/" + principal.getTenantId()
                    + "/" + temaVersao + "/logo.png"
                : request.getContextPath() + "/images/criare3d-128.jpg");
        if (principal != null && principal.getTenantId() != null) {
            model.addAttribute("temaCssUrl", request.getContextPath() + "/tema/"
                    + principal.getTenantId() + "/" + temaVersao + ".css");
        }
    }
}
