package br.com.criare3d.controller;

import jakarta.servlet.http.HttpServletRequest;
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
        else if (caminho.startsWith("/usuarios")) pagina = "usuarios";
        model.addAttribute("paginaAtiva", pagina);
    }
}
