package br.com.criare3d.controller;

import br.com.criare3d.security.UsuarioPrincipal;
import br.com.criare3d.service.TenantTemaCache;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavegacaoModelAdvice {

    private final TenantTemaCache tenantTemaCache;

    public NavegacaoModelAdvice(TenantTemaCache tenantTemaCache) {
        this.tenantTemaCache = tenantTemaCache;
    }

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
        else if (caminho.startsWith("/vendas-estoque")) pagina = "vendas-estoque";
        else if (caminho.startsWith("/historico")) pagina = "historico";
        else if (caminho.startsWith("/usuarios") || caminho.startsWith("/clientes")) pagina = "clientes";
        model.addAttribute("paginaAtiva", pagina);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        UsuarioPrincipal principal = autenticacao != null && autenticacao.isAuthenticated()
                && autenticacao.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal
                ? usuarioPrincipal : null;

        var tema = principal == null || principal.getTenantId() == null
                ? null : tenantTemaCache.buscarTema(principal.getTenantId()).orElse(null);
        String nomeTenant = tema == null || tema.getNome() == null || tema.getNome().isBlank()
                ? "Criare 3D" : tema.getNome();
        long temaVersao = tema == null || tema.getTemaVersao() == null
                ? 1L : Math.max(1L, tema.getTemaVersao());
        boolean tenantDisponivel = principal != null && principal.getTenantId() != null;
        boolean tenantTemLogo = tema != null && Boolean.TRUE.equals(tema.getTemLogo());
        model.addAttribute("tenantDisponivel", tenantDisponivel);
        model.addAttribute("tenantNome", nomeTenant);
        model.addAttribute("tenantTemLogo", tenantTemLogo);
        model.addAttribute("tenantLogoUrl", tenantDisponivel
                && tenantTemLogo
                ? request.getContextPath() + "/tema/" + principal.getTenantId()
                    + "/" + temaVersao + "/logo"
                : request.getContextPath() + "/images/criare3d-128.jpg");
        if (tenantDisponivel) {
            model.addAttribute("temaCssUrl", request.getContextPath() + "/tema/"
                    + principal.getTenantId() + "/" + temaVersao + ".css");
        }
    }
}
